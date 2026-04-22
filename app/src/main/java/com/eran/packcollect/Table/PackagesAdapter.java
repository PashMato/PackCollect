package com.eran.packcollect.Table;

import android.annotation.SuppressLint;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eran.packcollect.R;
import com.eran.packcollect.DataBase.Package;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class PackagesAdapter extends RecyclerView.Adapter<PackagesAdapter.ItemViewHolder> {
    public List<Package> Packages;
    private OnItemClick onClickListener;
    private String userUid;

    public PackagesAdapter(List<Package> Packages, OnItemClick onClickListener) {
        this.Packages = Packages;
        this.onClickListener = onClickListener;
        userUid = FirebaseAuth.getInstance().getUid();
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
        holder.Details.setText(item.description);
        holder.Location.setText(item.packageAddress != null ? item.packageAddress.address : "null");

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickListener.OnClick(item);
            }
        });

        // Check if the current user is the owner
        if (userUid.equals(item.ownerUid)) {
            holder.PackageIcon.setImageResource(R.drawable.ic_my_package);
        } else {
            holder.PackageIcon.setImageResource(R.drawable.ic_public_package);
        }
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
        TextView Details;
        TextView Location;
        ImageView PackageIcon;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            Details = itemView.findViewById(R.id.details_title);
            Location = itemView.findViewById(R.id.location_text);
            PackageIcon = itemView.findViewById(R.id.package_icon);
        }
    }
}

