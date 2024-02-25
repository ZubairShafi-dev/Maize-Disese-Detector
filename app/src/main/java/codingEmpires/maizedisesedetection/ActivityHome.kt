package codingEmpires.maizedisesedetection

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import codingEmpires.maizedisesedetection.databinding.ActivityDiseaseDetectionBinding
import codingEmpires.maizedisesedetection.databinding.ActivityHomeBinding

class ActivityHome : AppCompatActivity() {
    lateinit var binding: ActivityHomeBinding
    private lateinit var mContext: Context


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mContext=this@ActivityHome

        binding.btnDisease.setOnClickListener {
            startActivity(Intent(this.mContext,ActivityDiseaseDetection::class.java))
        }
    }
}