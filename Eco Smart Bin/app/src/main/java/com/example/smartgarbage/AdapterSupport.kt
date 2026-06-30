package com.example.candroid1
import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgarbage.Emergency
import com.example.smartgarbage.R

class AdapterSupport(val activity: Activity,val emergencies:ArrayList<Emergency>):
    RecyclerView.Adapter<AdapterSupport.mvh>() {
    class mvh(view:View):RecyclerView.ViewHolder(view) {
        val card:CardView=view.findViewById(R.id.card)
        val image:ImageView=view.findViewById(R.id.image)
        val text:TextView=view.findViewById(R.id.text)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterSupport.mvh {
        val view=activity.layoutInflater.inflate(R.layout.listsupport,parent,false)
        return mvh(view)
    }

    override fun onBindViewHolder(holder: AdapterSupport.mvh, position: Int) {
        holder.image.setImageResource(emergencies[position].image)
        holder.text.text=emergencies[position].name
        holder.card.setOnClickListener{
            val i=Intent(Intent.ACTION_DIAL,"tel:${emergencies[position].number}".toUri())
            activity.startActivity(i)
        }
    }

    override fun getItemCount()=emergencies.size

}
