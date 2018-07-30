package com.fnspl.hiplaedu_teacher.adapter;

/**
 * Created by User on 8/3/2017.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.model.ManualAttendance;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.fnspl.hiplaedu_teacher.utils.NetworkUtility;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;


public class ManualAttendanceAdapter extends RecyclerView.Adapter<ManualAttendanceAdapter.ViewHolder> {
    private List<ManualAttendance> values;
    private Context context ;
    private OnAttendanceClickListener mListener;


    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView tv_student_name;
        public View layout;
        public Button btn_request_received;
        public ImageView img_profile_pic;

        public ViewHolder(View v) {
            super(v);
            layout = v;

            tv_student_name = (TextView) v.findViewById(R.id.tv_student_name);
            btn_request_received = (Button) v.findViewById(R.id.btn_request_received);
            img_profile_pic = (ImageView) v.findViewById(R.id.img_profile_pic);
        }
    }

    public void notifyDataChange(List<ManualAttendance> data) {
        this.values = data ;
        notifyDataSetChanged();
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ManualAttendanceAdapter(Context context, List<ManualAttendance> myDataset) {
        this.context = context ;
        values = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ManualAttendanceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
        // create a new view
        LayoutInflater inflater = LayoutInflater.from(
                parent.getContext());
        View v =
                inflater.inflate(R.layout.item_manual_attendance, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        holder.btn_request_received.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener!=null){
                    mListener.onManualAttendanceRequest(position);
                }
            }
        });

        ImageLoader.getInstance().displayImage(NetworkUtility.IMAGE_BASEURL+""+values.get(position).getPhoto(),
                holder.img_profile_pic, CONST.ErrorWithLoaderNormalCorner);

        holder.tv_student_name.setText(""+values.get(position).getStudent_name());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return values.size();
    }

    public interface OnAttendanceClickListener{
        void onManualAttendanceRequest(int position);
    }

    public void setOnAttendanceClickListener(OnAttendanceClickListener mListener){
        this.mListener = mListener;
    }
}