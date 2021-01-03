package it.imt.erode.utopic.vnodelp;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.sbml.jsbml.util.compilers.ASTNodeValue;
import org.sbml.jsbml.util.compilers.FormulaCompiler;

public class MyFormulaCompilerForDouble extends FormulaCompiler {

	@Override
	public ASTNodeValue compile(double real, String units) {
		//return new ASTNodeValue(toString(Locale.ENGLISH, real), this);
		return new ASTNodeValue(toStringForDouble(Locale.ENGLISH, real), this);
	}
	
	/**
	 * Allows for {@link Locale}-dependent number formatting.
	 * @param locale
	 * @param value
	 * @return
	 */
	public static final String toStringForDouble(Locale locale, double value) {
		if (Double.isNaN(value)) {
			return "NaN";
		} else if (Double.isInfinite(value)) {
		  String infinity = "INF";
    		  return value < 0 ? '-' + infinity : infinity;
		}
		
		if (((int) value) - value == 0) {
			//return String.format("%d", Integer.valueOf((int) value));
			return String.valueOf(value);
		}

		if ((Math.abs(value) < 1E-4) || (1E4 < Math.abs(value))) {
			DecimalFormat df = new DecimalFormat(SCIENTIFIC_FORMAT,
					new DecimalFormatSymbols(locale));
			return df.format(value);
		}
		
		DecimalFormat df = new DecimalFormat(DECIMAL_FORMAT,
				new DecimalFormatSymbols(locale));
		return df.format(value);
	}
	
}
