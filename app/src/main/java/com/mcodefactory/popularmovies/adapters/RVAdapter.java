package com.mcodefactory.popularmovies.adapters;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mcodefactory.popularmovies.data.Movie;
import com.mcodefactory.popularmovies.R;
import com.mcodefactory.popularmovies.utils.NetworkUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.RVAdapterViewHolder> {
    private List<Movie> movies;
    private final RVAdapterOnClickHandler mClickHandler;

    public interface RVAdapterOnClickHandler {
        void onItemClick(Movie movie);
    }

    public RVAdapter(RVAdapterOnClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    public class RVAdapterViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
        public final ImageView mPoster;

        public RVAdapterViewHolder(View view) {
            super(view);
            mPoster= (ImageView) view.findViewById(R.id.iv_poster);
            view.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            Movie selectedMovie = movies.get(adapterPosition);
            mClickHandler.onItemClick(selectedMovie);
        }
    }

    @Override
    public RVAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.rv_layout;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, viewGroup, false);
        return new RVAdapterViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RVAdapterViewHolder adapterViewHolder, int position) {
        Movie movie = movies.get(position);
        if (movie.isFavorite()) {
            String imageName = movie.getPosterPath();
            File poster = loadPosterFromStorage(adapterViewHolder.itemView.getContext(), imageName);
            Picasso.with(adapterViewHolder.itemView.getContext()).load(poster).into(adapterViewHolder.mPoster);
        } else {
            Uri posterUri = NetworkUtils.buildPosterUri(movie.getPosterPath());
            Picasso.with(adapterViewHolder.itemView.getContext()).load(posterUri).
                    placeholder(R.drawable.ic_panorama_white_48px).into(adapterViewHolder.mPoster);
        }
    }


    @Override
    public int getItemCount() {
        if (null == movies) return 0;
        return movies.size();
    }


    public void setData(List<Movie> movieList) {
        movies = movieList;
        notifyDataSetChanged();
    }

    public File loadPosterFromStorage(Context context, String imageName) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        return new File(directory, imageName);
    }

}
