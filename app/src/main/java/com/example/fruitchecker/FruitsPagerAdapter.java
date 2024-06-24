package com.example.fruitchecker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import java.util.List;

public class FruitsPagerAdapter extends PagerAdapter {

    Context contextF;
    List<String> fruitsDetected;
    LayoutInflater inflater;

    public FruitsPagerAdapter(Context context, List<String> fruitsDetected) {
        this.contextF = context;
        this.fruitsDetected = fruitsDetected;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return fruitsDetected.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view.equals(object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = inflater.inflate(R.layout.results, container, false);
        TextView textViewResult = view.findViewById(R.id.textViewResult);
        textViewResult.setText(fruitsDetected.get(position));
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}