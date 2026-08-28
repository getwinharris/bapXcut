import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
public class ScratchTest {
    public void test(MediaSource s1, MediaSource s2) {
        new MergingMediaSource(true, true, s1, s2);
    }
}
