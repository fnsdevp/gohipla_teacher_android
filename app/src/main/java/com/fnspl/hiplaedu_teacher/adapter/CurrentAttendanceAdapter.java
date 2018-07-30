package com.fnspl.hiplaedu_teacher.adapter;

/**
 * Created by User on 8/3/2017.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.model.CurrentAttendanceData;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.fnspl.hiplaedu_teacher.utils.NetworkUtility;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;


public class CurrentAttendanceAdapter extends RecyclerView.Adapter<CurrentAttendanceAdapter.ViewHolder> {
    private List<CurrentAttendanceData> values;
    private Context context ;
    private OnAttendanceClickListener mListener;


    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView tv_student_name;
        public View layout;
        public ImageView iv_attendance_status, img_profile_pic;

        public ViewHolder(View v) {
            super(v);
            layout = v;

            tv_student_name = (TextView) v.findViewById(R.id.tv_student_name);
            iv_attendance_status = (ImageView) v.findViewById(R.id.iv_attendance_status);
            img_profile_pic = (ImageView) v.findViewById(R.id.img_profile_pic);
        }
    }

    public void notifyDataChange(List<CurrentAttendanceData> data) {
        this.values = data ;
        notifyDataSetChanged();
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public CurrentAttendanceAdapter(Context context, List<CurrentAttendanceData> myDataset) {
        this.context = context ;
        values = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public CurrentAttendanceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                  int viewType) {
        // create a new view
        LayoutInflater inflater = LayoutInflater.from(
                parent.getContext());
        View v =
                inflater.inflate(R.layout.item_attendance_row, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        ImageLoader.getInstance().displayImage(NetworkUtility.IMAGE_BASEURL+""+values.get(position).getPhoto(),
                holder.img_profile_pic, CONST.ErrorWithLoaderNormalCorner);

        holder.tv_student_name.setText(""+values.get(position).getStudent_name());
        if(values.get(position).getStatus()!=null) {
            if (values.get(position).getStatus().equalsIgnoreCase("present")) {
                holder.iv_attendance_status.setImageResource(R.drawable.attend);
                holder.iv_attendance_status.setBackground(context.getResources().getDrawable(R.drawable.green_button_small_radius));
            } else if (values.get(position).getStatus().equalsIgnoreCase("absent")) {
                holder.iv_attendance_status.setImageResource(R.drawable.absent);
                holder.iv_attendance_status.setBackground(context.getResources().getDrawable(R.drawable.yellow_button_small_radius));
            } else {
                holder.iv_attendance_status.setImageResource(R.drawable.noresponse);
                holder.iv_attendance_status.setBackground(context.getResources().getDrawable(R.drawable.black_button_small_radius));
            }
        }else {
            holder.iv_attendance_status.setImageResource(R.drawable.noresponse);
            holder.iv_attendance_status.setBackground(context.getResources().getDrawable(R.drawable.black_button_small_radius));
        }
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