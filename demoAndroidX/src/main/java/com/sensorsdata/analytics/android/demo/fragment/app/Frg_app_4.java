package com.sensorsdata.analytics.android.demo.fragment.app;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.BaseAppFragment;

public class Frg_app_4 extends BaseAppFragment {
    public Frg_app_4() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_frg_app_4, container, false);

        FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fl_frg_1, new Frg_app_1());
        fragmentTransaction.commit();

        v.findViewById(R.id.tv_frg_7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Frg_app_4", Toast.LENGTH_SHORT).show();
            }
        });
        return v;
    }
}
