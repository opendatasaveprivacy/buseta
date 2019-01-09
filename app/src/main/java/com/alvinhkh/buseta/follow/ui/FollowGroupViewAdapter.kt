package com.alvinhkh.buseta.follow.ui

import android.annotation.SuppressLint
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.model.FollowGroup
import android.widget.LinearLayout
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow
import java.lang.ref.WeakReference


class FollowGroupViewAdapter(
        private val follow: Follow,
        private val followDatabase: FollowDatabase,
        private val fragmentRef: WeakReference<BottomSheetDialogFragment>,
        private val data: MutableList<FollowGroup> = mutableListOf()
): RecyclerView.Adapter<FollowGroupViewAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position], follow, followDatabase, fragmentRef)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_follow_group_select, parent, false))
    }

    override fun getItemCount(): Int = data.size

    fun add(i: Int, f: FollowGroup) {
        data.add(i, f)
        notifyItemInserted(i)
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    fun replace(l: MutableList<FollowGroup>) {
        data.clear()
        data.addAll(l)
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(followGroup: FollowGroup, follow: Follow, followDatabase: FollowDatabase, fragmentRef: WeakReference<BottomSheetDialogFragment>) {
            itemView.findViewById<TextView>(R.id.name).text = when {
                followGroup.name.isNotEmpty() -> followGroup.name
                followGroup.id == FollowGroup.UNCATEGORISED -> itemView.context.getString(R.string.uncategorised)
                else -> followGroup.id
            }
            val followCount = followDatabase.followDao().count(followGroup.id, follow.companyCode,
                    follow.routeNo, follow.routeSeq, follow.routeServiceType, follow.stopId, follow.stopSeq)
            itemView.findViewById<ImageView>(R.id.icon).setImageResource(when {
                followGroup.id == "____clear" -> R.drawable.ic_outline_clear_24dp
                followGroup.id == "____add_new" -> R.drawable.ic_outline_add_box_24dp
                followCount > 0 -> R.drawable.ic_outline_check_box_24dp
                else -> R.drawable.ic_outline_check_box_outline_blank_24dp
            })
            itemView.setOnClickListener { view -> when {
                followGroup.id == "____clear" -> {
                    followDatabase.followDao().delete(follow.companyCode, follow.routeNo, follow.routeSeq,
                            follow.routeServiceType, follow.stopId, follow.stopSeq)
                    fragmentRef.get()?.dismiss()
                }
                followGroup.id == "____add_new" -> {
                    val builder = AlertDialog.Builder(view.context, R.style.AppTheme_Dialog)
                    builder.setTitle(R.string.add_new_group)
                    val layout = LinearLayout(builder.context)
                    layout.setPadding(48, 0, 48, 0)
                    layout.orientation = LinearLayout.VERTICAL
                    val textInputLayout = TextInputLayout(builder.context)
                    val textInputEditText = TextInputEditText(builder.context)
                    textInputEditText.setText("")
                    textInputLayout.addView(textInputEditText)
                    layout.addView(textInputLayout)
                    builder.setView(layout)
                    builder.setNegativeButton(R.string.action_cancel) { dialogInterface, _ -> dialogInterface.cancel() }
                    builder.setPositiveButton(R.string.action_confirm) { dialogInterface, _ ->
                        val name = textInputEditText.text.toString().trim()
                        if (name.isNotEmpty()) {
                            followDatabase.followGroupDao().insert(FollowGroup(name, name, ""))
                            follow.groupId = name
                            val inserted = followDatabase.followDao().insert(follow)
                            if (inserted > 0) {
                                itemView.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_outline_check_box_24dp)
                            }
                        }
                        dialogInterface.dismiss()
                    }
                    builder.create().show()
                }
                else -> {
                    follow.groupId = followGroup.id
                    val count = followDatabase.followDao().count(follow.groupId, follow.companyCode,
                            follow.routeNo, follow.routeSeq, follow.routeServiceType, follow.stopId, follow.stopSeq)
                    if (count > 0) {
                        val rowDeleted = followDatabase.followDao().delete(follow.groupId, follow.companyCode, follow.routeNo,
                                follow.routeSeq, follow.routeServiceType, follow.stopId, follow.stopSeq)
                        if (rowDeleted > 0) {
                            itemView.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_outline_check_box_outline_blank_24dp)
                        }
                    } else {
                        val inserted = followDatabase.followDao().insert(follow)
                        if (inserted > 0) {
                            itemView.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_outline_check_box_24dp)
                        }
                    }
                }
            }}
        }
    }

}