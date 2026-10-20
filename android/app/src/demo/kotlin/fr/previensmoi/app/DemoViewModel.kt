package fr.previensmoi.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.previensmoi.data.demo.DemoScenario
import fr.previensmoi.data.demo.DemoService
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Debug-only controls, compiled solely in the demo source set. */
@HiltViewModel
class DemoViewModel @Inject constructor(private val service: DemoService) : ViewModel() {
    val scenario = service.scenario
    fun select(value: DemoScenario) { viewModelScope.launch { service.selectScenario(value) } }
    fun simulateCheck() { viewModelScope.launch { service.simulateCheck() } }
    fun reset() { viewModelScope.launch { service.reset() } }
}
