
import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.lifecycle.ViewModel

class ViewModel(private val context: Context) : ViewModel() {

    // Get the current system brightness
    fun getCurrentBrightness(): Int {
        val contentResolver: ContentResolver = context.contentResolver
        return Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }

    // Set the system brightness
    fun setBrightness(brightness: Int) {
        val contentResolver: ContentResolver = context.contentResolver
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
    }

    // Get the current system volume
    fun getCurrentVolume(): Int {
        val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    // Set the system volume
    fun setVolume(volume: Int) {
        val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    // Method to decrease brightness or volume
    fun decreaseBrightnessOrVolume() {
        val currentBrightness = getCurrentBrightness()
        val currentVolume = getCurrentVolume()

        // Determine whether to adjust brightness or volume based on the current screen position
        if (currentBrightness > currentVolume) {
            // Decrease brightness
            val newBrightness = currentBrightness - 10 // Adjust the step as needed
            setBrightness(newBrightness)
            // Update UI or perform other actions as needed
        } else {
            // Decrease volume
            val newVolume = currentVolume - 1 // Adjust the step as needed
            setVolume(newVolume)
            // Update UI or perform other actions as needed
        }
    }

    // Method to increase brightness or volume
    fun increaseBrightnessOrVolume() {
        val currentBrightness = getCurrentBrightness()
        val currentVolume = getCurrentVolume()

        // Determine whether to adjust brightness or volume based on the current screen position
        if (currentBrightness > currentVolume) {
            // Increase brightness
            val newBrightness = currentBrightness + 10 // Adjust the step as needed
            setBrightness(newBrightness)
            // Update UI or perform other actions as needed
        } else {
            // Increase volume
            val newVolume = currentVolume + 1 // Adjust the step as needed
            setVolume(newVolume)
            // Update UI or perform other actions as needed
        }
    }
}
