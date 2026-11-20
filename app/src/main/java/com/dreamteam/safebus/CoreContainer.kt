package com.dreamteam.safebus

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dreamteam.safebus.fleet.data.RemoteFleetRepository
import com.dreamteam.safebus.fleet.ui.FleetViewModel
import com.dreamteam.safebus.iam.data.*
import com.dreamteam.safebus.iam.domain.*
import com.dreamteam.safebus.iam.ui.AuthViewModel
import com.dreamteam.safebus.safetycase.data.*
import com.dreamteam.safebus.safetycase.ui.*
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.SyncViewModel
import com.dreamteam.safebus.shared.ui.ServiceViewModel
import com.dreamteam.safebus.trip.data.*
import com.dreamteam.safebus.trip.domain.ActiveShift
import com.dreamteam.safebus.trip.domain.Position
import com.dreamteam.safebus.trip.ui.TripViewModel
import com.dreamteam.safebus.trip.ui.CountingViewModel
import com.dreamteam.safebus.trip.ui.CounterViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal inline fun <reified T : ViewModel> ViewModelStore.model(key: String, crossinline create: () -> T): T =
    ViewModelProvider(this, object : ViewModelProvider.Factory {
        override fun <V : ViewModel> create(modelClass: Class<V>): V = modelClass.cast(create())!!
    })[key, T::class.java]

class CoreContainer(val endpoint: String, private val localHttp: Boolean, private val store: SecureStore, private val engine: HttpEngine = UrlConnectionEngine(), private val scheduleSync: () -> Unit = {}) {
    val auth: RemoteAuthRepository
    private val models = ViewModelStore()
    @Volatile var account: AccountScope? = null
        private set
    private val api = ApiClient(endpoint, localHttp, { authToken() }, { invalidate(it) }, engine)
    init { auth = RemoteAuthRepository(HttpAuthService(api), store, endpoint) }
    val serviceModel by lazy {
        val service = HttpServiceContact(api)
        models.model("service") { ServiceViewModel(service, DurableContactRepository(service, store, endpoint)) }
    }
    private fun authToken() = auth.token()
    private fun invalidate(token: String) = auth.invalidate(token)
    val authModel by lazy {
        models.model("auth") { AuthViewModel(auth, {
            account?.stopTracking()
            DomainResult.Success(Unit)
        }) }
    }
    fun bind(session: Session): AccountScope {
        val previous = account
        if (previous?.session?.accessToken == session.accessToken) return previous
        previous?.dispose()
        return AccountScope(session, endpoint, localHttp, store, engine,
            { auth.session.value?.takeIf { it.accessToken == session.accessToken }?.let { auth.token() } }, auth::invalidate, scheduleSync).also { account = it }
    }
    fun detach() { account?.dispose(); account = null }
    fun dispose() { detach(); models.clear() }
}

class AccountScope(val session: Session, endpoint: String, localHttp: Boolean, store: SecureStore, engine: HttpEngine,
    token: () -> String?, unauthorized: (String) -> Unit, scheduleSync: () -> Unit = {}) {
    init { require(session.role != Role.PASSENGER) }
    private val owner = "$endpoint:${session.role}:${session.companyId}:${session.userId}"
    val api = ApiClient(endpoint, localHttp, token, unauthorized, engine)
    val outbox = Outbox(store, owner, PendingTransport { request -> api.call("POST", request.path, request.body, request.contentType) {} }, onPending = scheduleSync)
    val trip = RemoteTripRepository(HttpTripService(api, outbox), store, owner)
    @Volatile private var latestDriverPosition: Position? = null
    val safety = RemoteSafetyRepository(api, outbox, store, owner, driverPosition = { latestDriverPosition })
    val fleet = RemoteFleetRepository(api)
    val events = SupervisorEvents(endpoint, token, unauthorized)
    private val mutableShift = MutableStateFlow<ActiveShift?>(null)
    private val mutableTracking = MutableStateFlow(TrackingState())
    val shift = mutableShift.asStateFlow()
    val tracking = mutableTracking.asStateFlow()
    @Volatile var tracker: TrackingEngine? = null
        private set
    private val models = ViewModelStore()
    val tripModel = models.model("trip") { TripViewModel(trip, ::setShift) }
    val safetyModel = models.model("safety") { SafetyViewModel(safety) }
    val panicModel = models.model("panic") { PanicViewModel(safety) }
    val fleetModel = models.model("fleet") { FleetViewModel(fleet) }
    val syncModel = models.model("sync") { SyncViewModel(outbox::count, outbox::flush) }
    private val counting = RemoteCountingRepository(api)
    private val counterService = HttpCounterService(endpoint, localHttp, { session.role == Role.SUPERVISOR && token() != null }, engine)
    private val counterStore = store
    fun countingModel(busId: Long): CountingViewModel {
        require(session.role == Role.SUPERVISOR && busId > 0)
        return models.model("counting:$busId") { CountingViewModel(counting, busId) }
    }
    fun counterModel(busId: Long, shiftId: Long, sourceId: Long): CounterViewModel {
        require(session.role == Role.SUPERVISOR && busId > 0 && shiftId > 0 && sourceId > 0)
        return models.model("counter:$busId:$shiftId:$sourceId") { CounterViewModel(DurableCounterRepository(counterService, counterStore, "$owner:source:$sourceId", busId, shiftId), busId, shiftId) }
    }

    fun setShift(value: ActiveShift?) {
        if (mutableShift.value == value) return
        stopTracking(); mutableShift.value = value
        if (value != null) tracker = TrackingEngine(sendPosition = { trip.locate(value, it) })
        updateTracking()
    }
    fun updateTracking() { mutableTracking.value = tracker?.state?.value ?: TrackingState() }
    fun rememberDriverPosition(position: Position?) {
        latestDriverPosition = position?.takeIf { session.role == Role.DRIVER && mutableShift.value != null }
    }
    fun unavailable() { tracker?.unavailable(); updateTracking() }
    fun stopTracking() { tracker?.stop(); tracker = null; latestDriverPosition = null; mutableTracking.value = TrackingState() }
    fun dispose() { stopTracking(); models.clear() }
}
