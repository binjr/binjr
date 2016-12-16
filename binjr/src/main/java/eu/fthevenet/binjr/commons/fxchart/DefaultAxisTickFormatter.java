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

import org.gillius.jfxutils.chart.AxisTickFormatter;

import java.text.NumberFormat;

/**
 * DefaultAxisTickFormatter formats labels using a default number instance.
 *
 * @author Jason Winnebeck
 */
public class DefaultAxisTickFormatter implements AxisTickFormatter {
//	private final NumberFormat normalFormat = NumberFormat.getNumberInstance();
//	private final NumberFormat engFormat = new DecimalFormat( "0.###E0" );

	private NumberFormat currFormat = NumberFormat.getNumberInstance();

	public DefaultAxisTickFormatter() {
	}

	@Override
	public void setRange( double low, double high, double tickSpacing ) {
		//The below is an attempt as using engineering notation for large numbers, but it doesn't work.
//		currFormat = normalFormat;
//		double log10 = Math.log10( low );
//		if ( log10 < -4.0 || log10 > 5.0 ) {
//			currFormat = engFormat;
//		} else {
//			log10 = Math.log10( high );
//			if ( log10 < -4.0 || log10 > 5.0 ) {
//				currFormat = engFormat;
//			}
//		}

//		if (tickSpacing <= 10000.0)
//			currFormat = normalFormat;
//		else
//			currFormat = engFormat;
	}

	@Override
	public String format( Number value ) {
		return currFormat.format( value );
	}
}
