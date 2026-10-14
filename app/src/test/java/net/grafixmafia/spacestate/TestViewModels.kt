package net.grafixmafia.spacestate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds

/** Gives test models the same scope cleanup that their owner performs in the app. */
@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModels {
    private val store = ViewModelStore()
    private var nextKey = 0

    fun <T : ViewModel> track(model: T): T = model.also {
        store.put("test-model-${nextKey++}", it)
    }

    fun run(dispatcher: TestDispatcher, body: suspend TestScope.() -> Unit) =
        runTest(context = dispatcher, timeout = 10.seconds) {
            try {
                body()
            } finally {
                // Clear before runTest drains scheduled work: a frozen fake clock
                // otherwise leaves the model's expiry timer scheduling forever.
                store.clear()
                runCurrent()
            }
        }
}
