package com.sensorsdata.analytics.android.demo.fragment.androidx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.FragmentTransaction;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.BaseAndroidXFragment;

public class Frg_7 extends BaseAndroidXFragment {

    public Frg_7() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_frg_7, container, false);

        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fl_frg_1, new Frg_1());
        fragmentTransaction.commit();

        v.findViewById(R.id.tv_frg_7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Frg_7", Toast.LENGTH_SHORT).show();
            }
        });
        return v;
    }
}
