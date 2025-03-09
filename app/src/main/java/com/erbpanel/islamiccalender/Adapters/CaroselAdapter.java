package com.erbpanel.islamiccalender.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.erbpanel.islamiccalender.Models.CaroselItem;
import com.erbpanel.islamiccalender.R;

import java.util.List;

public class CaroselAdapter extends RecyclerView.Adapter<CaroselAdapter.ViewHolder> {
    private final List<CaroselItem> caroselList;

    public CaroselAdapter(List<CaroselItem> caroselItem) {
        this.caroselList = caroselItem;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.carosel_item_iftar_sehri, parent, false);

        // Force match_parent for ViewPager2
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        view.setLayoutParams(layoutParams);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CaroselItem item = caroselList.get(position);
        holder.hijriDate.setText(item.getHijriDate());
        holder.sehriTime.setText(item.getSehriTime());
        holder.iftarTime.setText(item.getIftarTime());
    }

    @Override
    public int getItemCount() {
        return caroselList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView hijriDate, sehriTime, iftarTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            hijriDate = itemView.findViewById(R.id.hijri_date);
            sehriTime = itemView.findViewById(R.id.sehri_time);
            iftarTime = itemView.findViewById(R.id.iftar_time);
        }
    }
}
