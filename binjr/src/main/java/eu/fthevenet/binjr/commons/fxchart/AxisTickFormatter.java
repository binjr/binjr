/*
 * Copyright 2013 Jason Winnebeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fthevenet.binjr.commons.fxchart;

import javafx.scene.chart.ValueAxis;

/**
 * AxisTickFormatter provides label formatting logic for a {@link ValueAxis}. Many formatters use the java.text.Format
 * implementations, which are typically not thread-safe. Therefore, most AxisTickFormatter instances are not
 * thread-safe for concurrent use.
 *
 * @author Jason Winnebeck
 */
public interface AxisTickFormatter {
	/**
	 * Sets the range of formatting. Some formatters use this to determine labels based on the scale of the range, i.e.
	 * picking 1,000,000 versus 1e6 or "January 2103" when a range is months wide versus "04:30" when a range is minutes
	 * wide.
	 *
	 * @param low         low value of the axis
	 * @param high        high value of the axis
	 * @param tickSpacing distance between each tick
	 */
	public void setRange(double low, double high, double tickSpacing);

	/**
	 * Given a number on the axis at a tick point, return a label.
	 */
	public String format(Number value);
}
