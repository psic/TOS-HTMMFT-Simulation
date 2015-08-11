package com.htmmft.video;


//TODO close the output file if input file is ended
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class VideoWriter extends Thread {

	static ViewerClientLogMain soccerMaster;


	private static final double FRAME_RATE = 20;

	private static final int SECONDS_TO_RUN_FOR = 20;

	private String outputFilename = "mydesktop.mp4";

	private IMediaWriter writer=null;
	
	private boolean end=false;
	
	private static int height;
	private static int width;

	public VideoWriter(ViewerClientLogMain soccerMaster){
		this.soccerMaster = soccerMaster;
		if (soccerMaster.getMatch() != null)
			outputFilename = ViewerClientLogMain.vidFolder + Integer.toString(soccerMaster.getMatch().getId())+".mp4";
		else
			outputFilename = "vid.mp4";
		//TODO verifier que les 2 sont paire
		height = soccerMaster.getContentPane().getHeight();
		width = soccerMaster.getContentPane().getWidth();
		if ( (height %2 )== 1)
			height = height +1;
		if ( (width %2 )== 1)
			width = width +1;
	}

	public void run(){
		System.out.println("Starting Video Writer");
		// let's make a IMediaWriter to write the file.
		writer = ToolFactory.makeWriter(outputFilename);

		// We tell it we're going to add one video stream, with id 0,
		// at position 0, and that it will have a fixed frame rate of FRAME_RATE.
		//writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4,screenBounds.width/2, screenBounds.height/2);

		//writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264,width/2,height/2);
		writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264,width,height);
		
		long startTime = System.nanoTime();
		while(!end){
			// take the screen shot
			BufferedImage screen = getDesktopScreenshot();
			// convert to the right image type
			BufferedImage bgrScreen = convertToType(screen, BufferedImage.TYPE_3BYTE_BGR);
			// encode the image to stream #0
			writer.encodeVideo(0, bgrScreen, System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			// sleep for frame rate milliseconds
			try {
				Thread.sleep((long) (1000 / FRAME_RATE));
				isfinish();
			} 
			catch (InterruptedException e) {
				return;

			}
		}
		System.out.println("Video Writer Ended");

	}

	private boolean isfinish() throws InterruptedException {
		if (end){
			throw new InterruptedException();
		}
		return true;
		
	}

	private static BufferedImage getDesktopScreenshot() {
		///Robot robot = new Robot();
		//Rectangle captureSize = new Rectangle(screenBounds);
		//return robot.createScreenCapture(captureSize);
		BufferedImage image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		// call the Component's paint method, using
		// the Graphics object of the image.
		soccerMaster.getContentPane().paint( image.getGraphics() );
		return image;

	}

	public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {

		BufferedImage image;

		// if the source image is already the target type, return the source image
		if (sourceImage.getType() == targetType) {
			image = sourceImage;
		}
		// otherwise create a new image of the target type and draw the new image
		else {
			image = new BufferedImage(sourceImage.getWidth(), 
					sourceImage.getHeight(), targetType);
			image.getGraphics().drawImage(sourceImage, 0, 0, null);
		}

		return image;
	}

	public void write() {
		if (soccerMaster.getMatch() != null)
			System.out.println("Write Video " + soccerMaster.getMatch().getId());
		// tell the writer to close and write the trailer if  needed
		writer.close();
		if (soccerMaster.getMatch() != null)
			System.out.println("Video Wrote " + soccerMaster.getMatch().getId());
		//this.interrupt();

	}

	public void end() {
		System.out.println("Put end to writer");
		end = true;
		
	}

}
