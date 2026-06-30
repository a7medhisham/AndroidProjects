package com.example.myapplication

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class Counter:ViewModel() {
     private val liveCounter=MutableLiveData(0)
     val newCounter:LiveData<Int> = liveCounter
    fun increment() {
        liveCounter.value=liveCounter.value?.plus(3)
    }
    fun increment12() {
        liveCounter.value=liveCounter.value?.plus(2)
    }
    fun increment13() {
        liveCounter.value=liveCounter.value?.plus(1)
    }
    fun increment10() {
        liveCounter.value = liveCounter.value?.minus(liveCounter.value!!)
    }

    private val liveCounter2=MutableLiveData(0)
    val newCounter2:LiveData<Int> = liveCounter2
    fun increment2() {
        liveCounter2.value=liveCounter2.value?.plus(3)
    }
    fun increment22() {
        liveCounter2.value=liveCounter2.value?.plus(2)
    }
    fun increment23() {
        liveCounter2.value=liveCounter2.value?.plus(1)
    }
    fun increment20() {
        liveCounter2.value = liveCounter2.value?.minus(liveCounter2.value!!)
    }

    fun determineWinner():String{
       return if(liveCounter.value!! > liveCounter2.value!!){
            "team A wins!"
        }else if(liveCounter.value!! < liveCounter2.value!!){
            "team B wins!"
        }else{
            " Draw!"
        }
    }
}