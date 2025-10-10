package Lista.compra

import android.os.Bundle
import com.journeyapps.barcodescanner.CaptureActivity

class LandscapeCaptureActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.custom_barcode_scanner)  // Layout personalizado
    }
}