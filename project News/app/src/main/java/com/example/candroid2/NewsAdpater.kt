package com.example.candroid2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.candroid2.databinding.ArticalListItemBinding

class NewsAdpater(val a: Activity, val articles: ArrayList<Article>) :
    Adapter<NewsAdpater.NewsViewHolder>() {
    class NewsViewHolder(val binding: ArticalListItemBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val b = ArticalListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(b)
    }

    override fun getItemCount()= articles.size


    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
       Log.d("trace","link: ${articles[position].urlToImage}")
        holder.binding.articaleText.text=articles[position].title
        Glide
            .with(holder.binding.articalImage.context)
            .load(articles[position].urlToImage)
            .error(R.drawable.brocken)
            .transition(DrawableTransitionOptions.withCrossFade(1000))
            .into(holder.binding.articalImage)
        val url =articles[position].url
        holder.binding.articleContainer.setOnClickListener {
            val i=Intent(Intent.ACTION_VIEW,url.toUri())
            a.startActivity(i)
        }
        holder.binding.shareFab.setOnClickListener {
            ShareCompat
                .IntentBuilder(a)
                .setType("text/plain")
                .setChooserTitle("share article with:")
                .setText(url)
                .startChooser()
        }
    }

}