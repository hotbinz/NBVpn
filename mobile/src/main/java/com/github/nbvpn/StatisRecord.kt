package com.github.nbvpn

import android.support.v7.util.SortedList
import com.github.nbvpn.utils.HostRecordEntity

class StatisRecord {
    companion object {
        private class BaseSorter<T: Comparable<T>> : SortedList.Callback<T>() {
            override fun onInserted(position: Int, count: Int) { }
            override fun areContentsTheSame(oldItem: T?, newItem: T?): Boolean = oldItem == newItem
            override fun onMoved(fromPosition: Int, toPosition: Int) { }
            override fun onChanged(position: Int, count: Int) { }
            override fun onRemoved(position: Int, count: Int) { }
            override fun areItemsTheSame(item1: T?, item2: T?): Boolean = item1 == item2
            override fun compare(o1: T?, o2: T?): Int =
                    if (o1 == null) if (o2 == null) 0 else 1 else if (o2 == null) -1 else compareNonNull(o1, o2)
            fun compareNonNull(o1: T, o2: T): Int = o1.compareTo(o2)
        }
        val hostRecord = SortedList(HostRecordEntity::class.java, BaseSorter<HostRecordEntity>())
    }
}