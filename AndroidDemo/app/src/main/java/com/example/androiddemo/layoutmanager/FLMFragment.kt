package com.example.androiddemo.layoutmanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.androiddemo.R
import com.example.androiddemo.ui.main.PlaceholderFragment

class FLMFragment: PlaceholderFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_flm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rlv:RecyclerView = view.findViewById(R.id.rlv)
        rlv.adapter = FLMAdapter()
        rlv.layoutManager = FlowLayoutManager()
    }

}

class FLMAdapter: RecyclerView.Adapter<FLMViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FLMViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_flm, parent, false)
        return FLMViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FLMViewHolder, position: Int) {
        val button = holder.itemView.findViewById<Button>(R.id.btn)
        val s = "${holder.itemView.resources.getString(R.string.app_name)}_$position"
        button.text = s
    }

    override fun getItemCount(): Int {
        return 60
    }

}

class FLMViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

}