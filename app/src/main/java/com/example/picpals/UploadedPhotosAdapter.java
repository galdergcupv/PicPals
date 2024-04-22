package com.example.picpals;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UploadedPhotosAdapter extends RecyclerView.Adapter<UploadedPhotosAdapter.ViewHolder> {
    private Context context;
    private List<String> base64Images;
    private List<String> imageNames;
    private List<String> uploaderNames;

    public UploadedPhotosAdapter(Context context, List<String> base64Images, List<String> imageNames, List<String> uploaderNames) {
        this.context = context;
        this.base64Images = base64Images;
        this.imageNames = imageNames;
        this.uploaderNames = uploaderNames;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_uploaded_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String base64Image = base64Images.get(position);
        Bitmap bitmap = decodeBase64(base64Image);
        holder.imageView.setImageBitmap(bitmap);

        String imageName = imageNames.get(position);
        holder.textViewImageName.setText(imageName);

        String uploaderName = uploaderNames.get(position);
        holder.textViewUploaderName.setText(uploaderName);
    }

    @Override
    public int getItemCount() {
        return base64Images.size();
    }

    private Bitmap decodeBase64(String base64Image) {
        byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textViewImageName;
        TextView textViewUploaderName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textViewImageName = itemView.findViewById(R.id.txtPhotoName);
            textViewUploaderName = itemView.findViewById(R.id.txtUploader);
        }
    }
}

