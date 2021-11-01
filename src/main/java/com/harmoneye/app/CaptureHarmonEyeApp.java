package com.harmoneye.app;

import com.harmoneye.analysis.AnalyzedFrame;
import com.harmoneye.audio.Capture;
import com.harmoneye.viz.Visualizer;
import com.pitchenga.Ptchng;

public class CaptureHarmonEyeApp extends AbstractHarmonEyeApp {

	private Capture capture;

	public CaptureHarmonEyeApp(Visualizer<AnalyzedFrame> visualizer2) {
        super(visualizer2);
        capture = new Capture(soundAnalyzer, AUDIO_SAMPLE_RATE, AUDIO_BITS_PER_SAMPLE);
	}

	public void start() {
		super.start();
		capture.start();
	}

	public void stop() {
		super.stop();
		capture.stop();
	}
}