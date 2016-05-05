package com.marktony.zhihudaily.UI.Fragments;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.marktony.zhihudaily.Adapters.LatestPostAdapter;
import com.marktony.zhihudaily.Entities.LatestPost;
import com.marktony.zhihudaily.Interfaces.OnRecyclerViewOnClickListener;
import com.marktony.zhihudaily.R;
import com.marktony.zhihudaily.UI.Activities.ReadActivity;
import com.marktony.zhihudaily.Utils.Api;
import com.rey.material.app.DatePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by lizhaotailang on 2016/3/21.
 */
public class LatestFragment extends Fragment {

    private RecyclerView rvLatestNews;
    private SwipeRefreshLayout refresh;
    private FloatingActionButton fab;
    private RequestQueue queue;
    private List<LatestPost> list = new ArrayList<LatestPost>();

    private LatestPostAdapter adapter;
    private LinearLayoutManager linearLayoutManager;

    private MaterialDialog dialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        linearLayoutManager = new LinearLayoutManager(getActivity());

        queue = Volley.newRequestQueue(getActivity().getApplicationContext());

        dialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.loading)
                .progress(true,0)
                .build();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_latest,container,false);

        initViews(view);

        dialog.show();

        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                load(null);
            }

        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 获取当前日期的前一天
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_MONTH,-1);
                final int year = c.get(Calendar.YEAR);
                final int month = c.get(Calendar.MONTH);
                final int day = c.get(Calendar.DAY_OF_MONTH);

                final DatePickerDialog dialog = new DatePickerDialog(getActivity());

                // 给dialog设置初始日期
                dialog.date(day,month,year);

                Calendar calendar = Calendar.getInstance();
                // 最小日期设置为2013年5月20日，知乎日报的诞生日期为2013年5月19日，如果传入的日期小于19，那么将会出现错误
                calendar.set(2013,5,20);
                // 通过calendar给dialog设置最大和最小日期
                // 其中最大日期为当前日期的前一天
                dialog.dateRange(calendar.getTimeInMillis(),Calendar.getInstance().getTimeInMillis() - 24*60*60*1000);
                dialog.show();

                dialog.positiveAction(getString(R.string.positive));
                dialog.negativeAction(getString(R.string.negative));

                dialog.positiveActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        load(parseDate(dialog.getDay(),dialog.getMonth(),dialog.getYear()));

                        dialog.dismiss();
                    }
                });

                dialog.negativeActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

            }
        });

        //设置下拉刷新的按钮的颜色
        refresh.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        //设置手指在屏幕上下拉多少距离开始刷新
        refresh.setDistanceToTriggerSync(300);
        //设置下拉刷新按钮的背景颜色
        refresh.setProgressBackgroundColorSchemeColor(Color.WHITE);
        //设置下拉刷新按钮的大小
        refresh.setSize(SwipeRefreshLayout.DEFAULT);


        load(null);

        rvLatestNews.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // 确保只有当recycler view的item滑动到最上面的一个时refresh layout才能下拉
                refresh.setEnabled(linearLayoutManager.findFirstVisibleItemPosition() == 0);
            }
        });

        return view;
    }

    private void initViews(View view) {

        rvLatestNews = (RecyclerView) view.findViewById(R.id.rv_main);
        rvLatestNews.setLayoutManager(linearLayoutManager);
        refresh = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);

    }

    /**
     * 用于加载最新日报或者历史日报
     * @param date 日期
     */
    private void load(String date){

        String url = null;

        if (date == null){
            url =  Api.LATEST;
        } else {
            url = Api.HISTORY + date;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {

                if ( !list.isEmpty()){
                    list.clear();
                }

                try {
                    if ( !jsonObject.getString("date").isEmpty()){

                        JSONArray array = jsonObject.getJSONArray("stories");
                        for (int i = 0; i < array.length(); i++){
                            JSONArray images = array.getJSONObject(i).getJSONArray("images");
                            List<String> stringList = new ArrayList<String>();
                            for (int j = 0; j < images.length(); j++){
                                String imgUrl = images.getString(j);
                                stringList.add(imgUrl);
                            }

                            LatestPost item = new LatestPost(
                                    array.getJSONObject(i).getString("title"),
                                    stringList,
                                    array.getJSONObject(i).getString("type"),
                                    array.getJSONObject(i).getString("id"));

                            list.add(item);
                        }
                    }

                    if (refresh.isRefreshing()){
                        Snackbar.make(refresh, R.string.refresh_done,Snackbar.LENGTH_SHORT).show();
                        refresh.setRefreshing(false);
                    }

                    adapter = new LatestPostAdapter(getActivity(),list);
                    rvLatestNews.setAdapter(adapter);
                    adapter.setItemClickListener(new OnRecyclerViewOnClickListener() {
                        @Override
                        public void OnItemClick(View v, int position) {
                            Intent intent = new Intent(getActivity(),ReadActivity.class);
                            intent.putExtra("id",list.get(position).getId());
                            intent.putExtra("title",list.get(position).getTitle());
                            startActivity(intent);
                        }
                    });

                    dialog.dismiss();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                dialog.dismiss();
            }
        });

        queue.add(request);
    }

    /**
     * 对传入的int型的日期转换为string类型
     * @param day 天数
     * @param month 月份
     * @param year 年份
     * @return 转换后的string类型的日期
     */
    private String parseDate(int day,int month,int year){
        String date = null;

        // month+1的原因为通过date picker dialog获取到的月份是从0开始的
        if (month <= 8 && day < 10){
            date = String.valueOf(year) + "0" + (month + 1) + "0" + day;
        } else if (month <= 8 && day >= 10){
            date = String.valueOf(year) + "0" + (month + 1) + day;
        } else if (month > 8 && day < 10){
            date = String.valueOf(year) + (month + 1) + "0" + day;
        } else {
            date = String.valueOf(year) + (month + 1) + day;
        }

        return date;
    }

}
