package com.eran.packcollect.Table;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eran.packcollect.R;
import com.eran.packcollect.DataBase.Package;

import java.util.List;

public class PackagesAdapter extends RecyclerView.Adapter<PackagesAdapter.ItemViewHolder> {
    public List<Package> Packages;
    private Context context;

    public PackagesAdapter(List<Package> Packages, Context c) {
        this.Packages = Packages;
        context = c;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.package_frame, parent,false);
        return new ItemViewHolder(view);
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {


        Package item = Packages.get(position);

        holder.packId = item.packageId;
        holder.Description.setText(item.description);
        holder.Location.setText(context.getString(R.string.location) + " " + (item.packageAddress != null ? item.packageAddress.address : "null"));
    }

    @Override
    public int getItemCount() {
        return Packages.size();
    }

    public Package getPackagesAt(int position) {
        return Packages.get(position);
    }


    class ItemViewHolder extends RecyclerView.ViewHolder {
        String packId;
        TextView Location;
        TextView Description;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            Description = itemView.findViewById(R.id.description_text);
            Location = itemView.findViewById(R.id.location_text);
        }
    }
}

