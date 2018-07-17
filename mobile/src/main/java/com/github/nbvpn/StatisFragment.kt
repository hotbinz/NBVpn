/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.github.nbvpn

import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import com.futuremind.recyclerviewfastscroll.FastScroller
import com.github.nbvpn.acl.CustomRulesFragment
import com.github.nbvpn.utils.HostRecordEntity
import com.github.nbvpn.utils.childFragManager

class StatisFragment : ToolbarFragment() {

    private val adapter by lazy { AclRulesAdapter() }
    private lateinit var list: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_statis, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.statis)

        list = view.findViewById(R.id.list)
        list.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        list.itemAnimator = DefaultItemAnimator()
        view.findViewById<FastScroller>(R.id.fastscroller).setRecyclerView(list)
        list.adapter = adapter
    }

    private inner class AclRulesAdapter : RecyclerView.Adapter<StatisFragment.AclRuleViewHolder>() {
        private val hosts = StatisRecord.hostRecord
        override fun onBindViewHolder(holder: StatisFragment.AclRuleViewHolder, i: Int) {
            holder.bind(hosts[i])
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AclRuleViewHolder(LayoutInflater
                .from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false))
        override fun getItemCount(): Int = hosts.size()

        fun add(hostRecordEntity: HostRecordEntity): Int {
            val old = hosts.size()
            val index = hosts.add(hostRecordEntity)
            if (old != hosts.size()) {
                notifyItemInserted(index)
            }
            return index
        }

    }

    override fun onStatisAdd(hostRecordEntity : HostRecordEntity) {
        adapter.add(hostRecordEntity)
    }

    private inner class AclRuleViewHolder(view: View) : RecyclerView.ViewHolder(view),
            View.OnClickListener, View.OnLongClickListener {
        lateinit var item: Any
        private val text = view.findViewById<TextView>(android.R.id.text1)

        init {
            view.setPaddingRelative(view.paddingStart, view.paddingTop,
                    Math.max(view.paddingEnd, resources.getDimensionPixelSize(R.dimen.fastscroll__bubble_corner)),
                    view.paddingBottom)
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
            view.setBackgroundResource(R.drawable.background_selectable)
        }

        fun bind(hostinfo: HostRecordEntity) {
            item = hostinfo
            text.text = hostinfo.hostname
        }

        override fun onClick(v: View?) {

        }

        override fun onLongClick(p0: View?): Boolean {
            return true
        }
    }


    override fun onDetach() {
        super.onDetach()
        childFragManager = null
    }

}
