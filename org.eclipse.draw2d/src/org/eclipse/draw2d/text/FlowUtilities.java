/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.draw2d.text;

import java.text.BreakIterator;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;

import org.eclipse.draw2d.FigureUtilities;

/**
 * Utility class for FlowFigures.
 * @author hudsonr
 * @since 2.1
 */
class FlowUtilities
	extends FigureUtilities
{

interface LookAhead {
	int getWidth();
}
private static int ELLIPSIS_SIZE;
private static final BreakIterator INTERNAL_LINE_BREAK = BreakIterator.getLineInstance();
private static TextLayout layout;

static final BreakIterator LINE_BREAK = BreakIterator.getLineInstance();

static boolean canBreakAfter(char c) {
	boolean result = Character.isWhitespace(c) || c == '-';
	if (!result && (c < 'a' || c > 'z')) {
		// chinese characters and such would be caught in here
		// LINE_BREAK is used here because INTERNAL_LINE_BREAK might be in use
		LINE_BREAK.setText(c + "a"); //$NON-NLS-1$
		result = LINE_BREAK.isBoundary(1);
	}
	return result;
}

private static int findFirstDelimeter(String string) {
	int macNL = string.indexOf('\r');
	int unixNL = string.indexOf('\n');
	
	if (macNL == -1)
		macNL = Integer.MAX_VALUE;
	if (unixNL == -1)
		unixNL = Integer.MAX_VALUE;

	return Math.min(macNL, unixNL);
}

/**
 * @param frag
 * @param font
 * @return
 * @since 3.1
 */
private static float getAverageCharWidth(TextFragmentBox fragment, Font font) {
	if (fragment.getWidth() > 0 && fragment.length != 0)
		return fragment.getWidth()/ (float)fragment.length;
	return getFontMetrics(font).getAverageCharWidth();
}

static int getBorderAscent(InlineFlow owner) {
	if (owner.getBorder() instanceof FlowBorder) {
		FlowBorder border = (FlowBorder)owner.getBorder();
		return border.getInsets(owner).top;
	}
	return 0;
}

static int getBorderAscentWithMargin(InlineFlow owner) {
	if (owner.getBorder() instanceof FlowBorder) {
		FlowBorder border = (FlowBorder)owner.getBorder();
		return border.getTopMargin() + border.getInsets(owner).top;
	}
	return 0;
}

static int getBorderDescent(InlineFlow owner) {
	if (owner.getBorder() instanceof FlowBorder) {
		FlowBorder border = (FlowBorder)owner.getBorder();
		return border.getInsets(owner).bottom;
	}
	return 0;
}

static int getBorderDescentWithMargin(InlineFlow owner) {
	if (owner.getBorder() instanceof FlowBorder) {
		FlowBorder border = (FlowBorder)owner.getBorder();
		return border.getBottomMargin() + border.getInsets(owner).bottom;
	}
	return 0;
}

/**
 * Provides a TextLayout that can be used by the Draw2d text package for Bidi.  Note that
 * orientation of the provided TextLayout could be LTR or RTL.  Clients should set the
 * orientation as desired before using the TextLayout.  This TextLayout should not
 * be disposed by clients.  To prevent Strings from sticking around in memory, clients
 * should also set the text for the provided TextLayout to be an empty String once they
 * are done using it.
 * 
 * @return an SWT TextLayout that can be used for Bidi
 * @since 3.1
 */
static TextLayout getTextLayout() {
	if (layout == null)
		layout = new TextLayout(Display.getDefault());
	return layout;
}

/**
 * @param frag
 * @param string
 * @param font
 * @since 3.1
 */
private static void initBidi(TextFragmentBox frag, String string, Font font) {
	if (frag.requiresBidi()) {
		TextLayout textLayout = getTextLayout();
		textLayout.setFont(font);
		//$TODO need to insert overrides in front of string.
		textLayout.setText(string);
	}
}

/**
 * @param string
 * @param guess
 * @param font
 * @return
 * @since 3.1
 */
private static int measureString(TextFragmentBox frag, String string, int guess, Font font) {
	if (frag.requiresBidi())
		return getTextLayout().getBounds(0, guess - 1).width;
	else
		return getStringDimension(string.substring(0, guess), font).x;
}

static void setupFragment(TextFragmentBox frag, Font f, String s) {
	if (frag.getWidth() == -1 || frag.isTruncated()) {
		int width;
		if (s.length() == 0 || frag.length == 0)
			width = 0;
		else if (frag.requiresBidi()) {
			TextLayout textLayout = getTextLayout();
			textLayout.setFont(f);
			textLayout.setText(s);
			width = textLayout.getBounds(0, frag.length - 1).width;
		} else
			width = getStringDimension(s.substring(0, frag.length), f).x;
		if (frag.isTruncated())
			width += ELLIPSIS_SIZE;
		frag.setWidth(width);
	}
}

/**
 * Sets up a fragment and returns the number of characters consumed from the given
 * String. An average character width can be provided as a hint for faster calculation. 
 * If a fragment's bidi level is set, a TextLayout will be used to calculate the width.
 * 
 * @param frag the TextFragmentBox
 * @param string the String
 * @param font the Font used for measuring
 * @param availableWidth the available width in pixels
 * @param wrapping the word wrap style
 * @return the number of characters that will fit in the given space; can be 0 (eg., when
 * the first character of the given string is a newline)
 */
public static int wrapFragmentInContext(TextFragmentBox frag, String string,
		FlowContext context, LookAhead lookahead, Font font, int wrapping) {
	frag.setTruncated(false);
	int strLen = string.length();
	if (strLen == 0) {
		frag.setWidth(-1);
		frag.length = 0;
		setupFragment(frag, font, string);
//		context.addToCurrentLine(frag);
		return 0;
	}
	
	INTERNAL_LINE_BREAK.setText(string);

	initBidi(frag, string, font);
	float avgCharWidth = getAverageCharWidth(frag, font);
	frag.setWidth(-1);
	
	/*
	 * Setup initial boundaries within the string.
	 */
	int absoluteMin = 0;
	int max, min = 1;
	if (wrapping == ParagraphTextLayout.WORD_WRAP_HARD) {
		absoluteMin = INTERNAL_LINE_BREAK.next();
		while (absoluteMin > 0 && Character.isWhitespace(string.charAt(absoluteMin - 1)))
			absoluteMin--;
		min = Math.max(absoluteMin, 1);
	}
	int firstDelimiter = findFirstDelimeter(string);
	if (firstDelimiter == 0)
		min = max = 0;
	else
		max = Math.min(strLen, firstDelimiter) + 1;

	
	int availableWidth = context.getRemainingLineWidth();
	int guess = 0, guessSize = 0;
	
	while (true) {
		if ((max - min) <= 1) {
			if (min == absoluteMin
					&& context.isCurrentLineOccupied() 
					&& !context.getContinueOnSameLine()
					&& availableWidth < measureString(frag, string, min, font)
						+ ((min == strLen && lookahead != null) ? lookahead.getWidth() : 0)
			) {
				context.endLine();
				availableWidth = context.getRemainingLineWidth();
				max = Math.min(strLen, firstDelimiter) + 1;
				if ((max - min) <= 1)
					break;
			} else
				break;
		}
		// Pick a new guess size
		// New guess is the last guess plus the missing width in pixels
		// divided by the average character size in pixels
		guess += 0.5f + (availableWidth - guessSize) / avgCharWidth;

		if (guess >= max) guess = max - 1;
		if (guess <= min) guess = min + 1;

		guessSize = measureString(frag, string, guess, font);
		
		if (guess == strLen
				&& lookahead != null
				&& !canBreakAfter(string.charAt(strLen - 1))
				&& guessSize + lookahead.getWidth() > availableWidth) {
			max = guess;
			continue;
		}

		if (guessSize <= availableWidth) {
			min = guess;
			frag.setWidth(guessSize);
			if (guessSize == availableWidth)
				max = guess + 1;
		} else
			max = guess;
	}
	
	int result = min;
 
	if (min == strLen) {
		//Everything fits
		if (string.charAt(strLen - 1) == ' ' 
				&& lookahead.getWidth() > availableWidth - frag.getWidth()) {
			frag.length = result - 1;
			frag.setWidth(-1);
		} else
			frag.length = result;
	} else if (min == firstDelimiter) {
		//move result past the delimiter
		frag.length = result;
		if (string.charAt(min) == '\r') {
			result++;
			if (++min < strLen && string.charAt(min) == '\n')
				result++;
		} else if (string.charAt(min) == '\n')
			result++;
	} else out: if (string.charAt(min) == ' '
			|| canBreakAfter(string.charAt(min - 1))
			|| INTERNAL_LINE_BREAK.isBoundary(min)) {
		frag.length = min;
		if (string.charAt(min) == ' ')
			result++;
		else if (string.charAt(min - 1) == ' ') {
			frag.length--;
			frag.setWidth(-1);
		}
	} else {
		// In the middle of an unbreakable offset
		result = INTERNAL_LINE_BREAK.preceding(min);
		if (result == 0) {
			switch (wrapping) {
				case ParagraphTextLayout.WORD_WRAP_TRUNCATE :
					ELLIPSIS_SIZE = FigureUtilities
							.getStringExtents(TextFlow.ELLIPSIS, font).width;
					int truncatedWidth = availableWidth - ELLIPSIS_SIZE;
					if (truncatedWidth > 0) {
						//$TODO this is very slow.  It should be using avgCharWidth to go faster
						while (min > 0) {
							guessSize = measureString(frag, string, min, font);
							if (guessSize <= truncatedWidth)
								break;
							min--;
						}
						frag.length = min;
					} else
						frag.length = 0;
					frag.setTruncated(true);
					result = INTERNAL_LINE_BREAK.following(max - 1);
					break out;

				default:
				case ParagraphTextLayout.WORD_WRAP_SOFT :
					result = min;
					break;
			}
		}
		frag.length = result;
		if (string.charAt(result - 1) == ' ')
			frag.length--;
		frag.setWidth(-1);
	}
	
	setupFragment(frag, font, string);
	return result;
}

}