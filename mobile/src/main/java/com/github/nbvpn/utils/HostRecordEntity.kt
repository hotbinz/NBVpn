package com.github.nbvpn.utils

import java.util.*

class HostRecordEntity(hostname:String, datetime: Date) : Comparable<HostRecordEntity>{
    var hostname:String? = hostname
    var datetime = datetime
    override fun compareTo(other: HostRecordEntity): Int {
        return other.datetime.compareTo(datetime)
    }
}