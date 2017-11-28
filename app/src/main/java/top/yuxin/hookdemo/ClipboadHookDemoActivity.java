package top.yuxin.hookdemo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ClipboadHookDemoActivity extends AppCompatActivity {

    private Button btn_copy;
    private Button btn_paste;
    private EditText edit_input;
    private TextView tv_got;
    private String input;
    private ClipboardManager clipboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clipboad_hook_demo);

        edit_input = (EditText)this.findViewById(R.id.edit_input);
        btn_copy = (Button)this.findViewById(R.id.btn_copy);
        btn_paste = (Button)this.findViewById(R.id.btn_paste);
        tv_got = (TextView)this.findViewById(R.id.tv_got);

        clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);

        btn_copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                input = edit_input.getText().toString().trim();
                if (TextUtils.isEmpty(input)) {
                    Toast.makeText(ClipboadHookDemoActivity.this, "input不能为空", Toast.LENGTH_SHORT).show();
                }
                //复制
                ClipData clip = ClipData.newPlainText("simpletext", input);

                clipboard.setPrimaryClip(clip);
            }
        });

        btn_paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //黏贴
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    tv_got.setText(clip.getItemAt(0).getText()+"");
                }
            }
        });


    }
}
