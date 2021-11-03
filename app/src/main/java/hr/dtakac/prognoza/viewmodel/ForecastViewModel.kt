package hr.dtakac.prognoza.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hr.dtakac.prognoza.model.database.ForecastMeta
import hr.dtakac.prognoza.model.database.Place
import hr.dtakac.prognoza.common.utils.hasExpired
import hr.dtakac.prognoza.common.utils.toErrorResourceId
import hr.dtakac.prognoza.model.repository.CachedSuccess
import hr.dtakac.prognoza.model.repository.Empty
import hr.dtakac.prognoza.model.repository.ForecastResult
import hr.dtakac.prognoza.model.repository.Success
import hr.dtakac.prognoza.repository.place.PlaceRepository
import hr.dtakac.prognoza.repository.preferences.PreferencesRepository
import hr.dtakac.prognoza.model.ui.MeasurementUnit
import hr.dtakac.prognoza.model.ui.forecast.EmptyForecastUiModel
import hr.dtakac.prognoza.model.ui.forecast.ForecastUiModel
import hr.dtakac.prognoza.model.ui.forecast.OutdatedForecastUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class ForecastViewModel<T : ForecastUiModel>(
    coroutineScope: CoroutineScope?,
    protected val preferencesRepository: PreferencesRepository,
    protected val placeRepository: PlaceRepository
) : CoroutineScopeViewModel(coroutineScope) {
    private var currentMeta: ForecastMeta? = null
    private var currentUnit: MeasurementUnit? = null

    protected abstract val _forecast: MutableLiveData<T>
    val forecast: LiveData<T> get() = _forecast

    protected lateinit var selectedPlace: Place

    private val _emptyScreen = MutableLiveData<EmptyForecastUiModel?>()
    val emptyScreen: LiveData<EmptyForecastUiModel?> get() = _emptyScreen

    private val _outdatedForecastMessage = MutableLiveData<OutdatedForecastUiModel?>(null)
    val outdatedForecastMessage: LiveData<OutdatedForecastUiModel?> get() = _outdatedForecastMessage

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun getForecast() {
        coroutineScope.launch {
            if (isReloadNeeded()) {
                _isLoading.value = true
                selectedPlace = placeRepository.get(preferencesRepository.getSelectedPlaceId())
                    ?: placeRepository.getDefaultPlace()
                when (val result = getNewForecast()) {
                    is Success -> handleSuccess(result)
                    is CachedSuccess -> handleCachedSuccess(result)
                    is Empty -> handleEmpty(result)
                }
                _isLoading.value = false
            }
        }
    }

    protected abstract suspend fun getNewForecast(): ForecastResult

    protected abstract suspend fun mapToForecastUiModel(success: Success, unit: MeasurementUnit): T

    private suspend fun handleSuccess(success: Success) {
        val selectedUnit = preferencesRepository.getSelectedUnit()
        _forecast.value = mapToForecastUiModel(success, selectedUnit)
        currentMeta = success.meta
        currentUnit = selectedUnit
        _emptyScreen.value = null
        _outdatedForecastMessage.value = null
    }

    private fun handleEmpty(empty: Empty) {
        _emptyScreen.value = EmptyForecastUiModel(empty.reason?.toErrorResourceId())
    }

    private suspend fun handleCachedSuccess(cachedResult: CachedSuccess) {
        handleSuccess(cachedResult.success)
        _outdatedForecastMessage.value = OutdatedForecastUiModel(
            reason = cachedResult.reason?.toErrorResourceId()
        )
    }

    private suspend fun isReloadNeeded(): Boolean {
        return currentMeta?.hasExpired() != false ||
                currentMeta?.placeId != preferencesRepository.getSelectedPlaceId() ||
                currentUnit != preferencesRepository.getSelectedUnit() ||
                _forecast.value == null
    }
}