package com.techhounds.imgcv.utils;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

/**
 * A helper class to aid in drawing onto an OpenCV image.
 * 
 * <p>
 * This class was created mainly because I kept forgetting what OpenCV calls I
 * needed to make to draw text and lines onto an image.
 * </p>
 * 
 * @author Paul Blankenbaker
 */
public class DrawTool {
	/** Foreground color. */
	private Scalar color;
	/** Backgound color for fills. */
	private Scalar bg;
	/** Thickness of lines (in pixels). */
	private int thickness;
	/** Font face to use when drawing text. */
	private int fontFace;
	/** How much to scale the font by. */
	private double fontScale;
	/**
	 * Really haven't tracked the details down on this - related to text
	 * positioning.
	 */
	private int[] baseline;

	/**
	 * Constructs a new instance with the default settings (green lines, black
	 * fill, 1 pixel thick lines and some default font).
	 */
	public DrawTool() {
		this(ScalarColors.GREEN, 1);
	}

	/**
	 * Construct a new instance with specific color and line thickness values.
	 * 
	 * @param color
	 *            Foreground (line) color to use (should not be null).
	 * @param thickness
	 *            How thick the lines should be (in pixels).
	 */
	public DrawTool(Scalar color, int thickness) {
		this.color = color;
		this.thickness = thickness;
		this.bg = null;
		this.fontFace = Core.FONT_HERSHEY_PLAIN;
		this.fontScale = 1.0;
		baseline = new int[] { 0 };
	}

	/**
	 * Draws a line between two points using the current color and line
	 * thickness.
	 * 
	 * @param img
	 *            The image to draw on.
	 * @param p0
	 *            The starting point of the line.
	 * @param p1
	 *            The ending point of the line.
	 */
	public void drawLine(Mat img, Point p0, Point p1) {
		Core.line(img, p0, p1, color, thickness);
	}

	/**
	 * Set the color for lines and text.
	 * 
	 * @param color
	 *            New color to use (must not be null).
	 */
	public void setColor(Scalar color) {
		if (color == null) {
			throw new NullPointerException();
		}
		this.color = color;
	}

	/**
	 * Get the current color for lines and text.
	 * 
	 * @return The current color being used.
	 */
	public Scalar getColor() {
		return color;
	}

	/**
	 * Set the background color to use for fill when drawing text.
	 * 
	 * @param color
	 *            The color for the background fill of text drawing (or null if
	 *            you don't want the background filled).
	 */
	public void setBackgroundColor(Scalar color) {
		this.bg = color;
	}

	/**
	 * Get the background fill color.
	 * 
	 * @return Color used for fills (or null if fill disabled).
	 */
	public Scalar getBackgroundColor() {
		return bg;
	}

	/**
	 * Set the thickness of lines in pixels.
	 * 
	 * @param thickness
	 *            How thick you want lines in pixels.
	 */
	public void setThickness(int thickness) {
		this.thickness = thickness;
	}

	/**
	 * Get the current line thickness (in pixels).
	 * 
	 * @return Thickness of lines.
	 */
	public int getThickness() {
		return thickness;
	}

	/**
	 * Determines the size required to draw text onto an image with the current
	 * settings.
	 * 
	 * @param text
	 *            The text you want to draw.
	 * @return A size in pixels required to draw the text.
	 */
	public Size getTextSize(String text) {
		Size size = Core.getTextSize(text, fontFace, fontScale, thickness,
				baseline);
		return size;
	}

	/**
	 * Draws text onto an image given the top left anchor point.
	 * 
	 * <p>
	 * NOTE: If the background color has been set to a non null value, the
	 * rectangle containing the text will be filled prior to drawing the text.
	 * </p>
	 * 
	 * @param img
	 *            The image to draw the text on.
	 * @param text
	 *            The text to draw.
	 * @param x
	 *            The left anchor point for placing the text.
	 * @param y
	 *            The top anchor point for placing the text.
	 */
	public void drawTextTopLeft(Mat img, String text, int x, int y) {
		Size size = getTextSize(text);
		Point botLeft = new Point(x, y + size.height);
		Point topRight = new Point(x + size.width, y);

		if (bg != null) {
			Core.rectangle(img, botLeft, topRight, bg, Core.FILLED);
		}

		Core.putText(img, text, botLeft, fontFace, fontScale, color);
	}

}
