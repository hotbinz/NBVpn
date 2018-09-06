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

import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import com.futuremind.recyclerviewfastscroll.FastScroller
import com.github.nbvpn.utils.FormetFileSize
import com.github.nbvpn.utils.HostRecordEntity
import com.github.nbvpn.utils.childFragManager
import java.text.SimpleDateFormat
import android.support.v7.app.AlertDialog

class StatisFragment : ToolbarFragment(), Toolbar.OnMenuItemClickListener {
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        val activity = requireActivity()
        AlertDialog.Builder(activity)
                .setTitle("确定要清空所有记录？")
                .setPositiveButton(R.string.yes, { _, _ ->
                    ToolbarFragment.Data.hostsRecordList.clear()
                })
                .setNegativeButton(R.string.no, null)
                .create()
                .show()
        return true
    }

    private val adapter by lazy { AclRulesAdapter() }
    private lateinit var list: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_statis, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.statis)
        toolbar.inflateMenu(R.menu.statis_rules_menu)
        toolbar.setOnMenuItemClickListener(this)
        list = view.findViewById(R.id.list)
        list.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        list.itemAnimator = DefaultItemAnimator()
        view.findViewById<FastScroller>(R.id.fastscroller).setRecyclerView(list)
        list.adapter = adapter
    }

    private inner class AclRulesAdapter : RecyclerView.Adapter<StatisFragment.AclRuleViewHolder>() {
        private val hosts = ToolbarFragment.Data.hostsRecordList
        override fun onBindViewHolder(holder: StatisFragment.AclRuleViewHolder, i: Int) {
            holder.bind(hosts[i])
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AclRuleViewHolder(LayoutInflater
                .from(parent.context).inflate(R.layout.layout_statis_item, parent, false))
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
        ToolbarFragment.Data.AddStatis(hostRecordEntity)
    }

    private inner class AclRuleViewHolder(view: View) : RecyclerView.ViewHolder(view),
            View.OnClickListener, View.OnLongClickListener {
        lateinit var item: Any
        private val text = view.findViewById<TextView>(android.R.id.text1)
        private val text2 = view.findViewById<TextView>(android.R.id.text2)
        private val text3 = view.findViewById<TextView>(R.id.text3)

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
            var hostAry:List<String> = hostinfo.hostname!!.split("/")
            text.text = hostAry[1]
            if (hostAry[0] == "bypass") {
                text3.text = "直连"
                text3.setBackgroundColor(Color.parseColor("#515151"))
            }
            else {
                text3.text = "代理"
                text3.setBackgroundColor(Color.parseColor("#1478b7"))
            }

            val df = SimpleDateFormat("HH:mm:ss")
            text2.text =  "#" + df.format(hostinfo.datetime) + " " + " - " + FormetFileSize(hostAry[2].toLong())
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
