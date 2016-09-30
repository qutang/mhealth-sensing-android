package io.github.qutang.sensing;

import android.graphics.Color;
import android.hardware.SensorEvent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.vision.text.Line;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    @BindView(R.id.phone_accel_chart) LineChart phoneAccelChart;

    private LineDataSet phoneX;
    private LineDataSet phoneY;
    private LineDataSet phoneZ;

    private LineData phoneData;

    @BindView(R.id.watch_accel_chart) LineChart watchAccelChart;

    private LineDataSet watchX;
    private LineDataSet watchY;
    private LineDataSet watchZ;

    private int count = 0;

    public MainActivityFragment() {
    }

    private void setupPhoneLineChart(){


        phoneX = new LineDataSet(new ArrayList<Entry>(), "Phone X");
        phoneY = new LineDataSet(new ArrayList<Entry>(), "Phone Y");
        phoneZ = new LineDataSet(new ArrayList<Entry>(), "Phone Z");
        phoneX.addEntry(new Entry(0, 0));
        phoneY.addEntry(new Entry(0,0));
        phoneZ.addEntry(new Entry(0, 0));
        phoneX.setColor(Color.parseColor("#FFCDD2"));
        phoneX.setDrawValues(false);
        phoneX.setLineWidth(2.5f);
        phoneX.setDrawCircles(true);
        phoneX.setCircleColor(Color.parseColor("#FFCDD2"));
        phoneX.setAxisDependency(YAxis.AxisDependency.LEFT);
        phoneY.setColor(Color.parseColor("#B3E5FC"));
        phoneY.setLineWidth(2.5f);
        phoneY.setDrawValues(false);
        phoneY.setDrawCircles(true);
        phoneY.setCircleColor(Color.parseColor("#B3E5FC"));
        phoneY.setAxisDependency(YAxis.AxisDependency.LEFT);
        phoneZ.setColor(Color.parseColor("#C8E6C9"));
        phoneZ.setLineWidth(2.5f);
        phoneZ.setDrawCircles(true);
        phoneZ.setCircleColor(Color.parseColor("#C8E6C9"));
        phoneZ.setAxisDependency(YAxis.AxisDependency.LEFT);
        phoneZ.setDrawValues(false);
        List<ILineDataSet> phoneDataset = new ArrayList<ILineDataSet>();
        phoneDataset.add(phoneX);
        phoneDataset.add(phoneY);
        phoneDataset.add(phoneZ);
        phoneData = new LineData(phoneDataset);
        phoneAccelChart.setData(phoneData);
        phoneAccelChart.setDescription("Phone Accelerometer");
        phoneAccelChart.setDrawGridBackground(false);
        phoneAccelChart.setDrawBorders(false);
        phoneAccelChart.setTouchEnabled(false);
        phoneAccelChart.getXAxis().setDrawGridLines(false);
        phoneAccelChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        phoneAccelChart.getAxisLeft().setDrawZeroLine(true);
        phoneAccelChart.getAxisRight().setDrawLabels(false);
        phoneAccelChart.getLegend().setEnabled(true);
        phoneAccelChart.setVisibleXRangeMaximum(20);
        phoneAccelChart.invalidate();
        count = 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        setupPhoneLineChart();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void updatePhoneAccelChart(SensorEvent lastEvent){
        float ts = count++;
        phoneX.addEntry(new Entry(ts, lastEvent.values[0] / 9.81f));
        phoneY.addEntry(new Entry(ts, lastEvent.values[1] / 9.81f));
        phoneZ.addEntry(new Entry(ts, lastEvent.values[2] / 9.81f));
        phoneData.notifyDataChanged();
        phoneAccelChart.notifyDataSetChanged();
        phoneAccelChart.setVisibleXRangeMaximum(20);
        phoneAccelChart.moveViewToX(ts);
        phoneAccelChart.invalidate();
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void resetPhoneAccelChart(ResetChartEvent event){
        setupPhoneLineChart();
    }

    @Subscribe
    public void updateWatchChart(SensorEvent lastEvent){

    }
}
