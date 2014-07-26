package mesquite.zephyr.lib;

import mesquite.lib.*;;

/**For making more informative shortened names.  Uses the first four characters of a taxon name and the taxon number.  Compare with SimpleTaxonNamer*/
public class TaxonNameShortener extends TaxonNamer {
	int startLength = 4;
	public boolean initialize(Taxa taxa){
		return true;
	}

	/*.................................................................................................................*/
	public String getNameToUse(Taxon taxon) {
		Taxa taxa = taxon.getTaxa();
		return getNameString(taxa, taxon);
	}

	/*.................................................................................................................*/
	public String getNameToUse(Taxa taxa, int it) {
		Taxon taxon = taxa.getTaxon(it);
		return getNameString(taxa, taxon);
	}

	/*.................................................................................................................*/
	private String getNameString(Taxa taxa, Taxon taxon){
		int numTaxa = taxa.getNumTaxa();
		String initialName = taxon.getName();
		int taxonNumber = taxon.getNumber();
		String startString = "";
		char leadingChar = initialName.charAt(0);
		//Don't want the name to start with anything but an alphabetic character (no numbers or symbols)
		if(!Character.isLetter(leadingChar)){
			initialName = "T" + initialName;
		}

		//Start of name should be four alpha-numeric characters.  Skip any characters that are not alpha-numeric.
		int currentChar = 0;
		while(startString.length() < startLength && currentChar < initialName.length()){
			char testChar = initialName.charAt(currentChar);
			if(Character.isLetterOrDigit(testChar)){
				startString += testChar;
			}
			currentChar++;
		}
		
		//Start of name should not be less than four characters.  Append with appropriate number of zeros if it is less than four.
		while(startString.length() < startLength){
			startString += "0";
		}

		int totalDigits = (int)(Math.log10((double)numTaxa)) + 1;

		String numberString = String.valueOf(taxonNumber);
		while(numberString.length() < totalDigits){
			numberString = "0" + numberString;
		}
		return startString + numberString;
	}
	/*.................................................................................................................*/
//TODO: Going to need to redo this, too.  If numbering starts with 1 (not zero), then need to remember to return to 0-start numbering when returning taxon number
	public int whichTaxonNumber(Taxa taxa, String name) {
		if (StringUtil.notEmpty(name) && name.length()>=2) {
			//check to make sure names are still in correct format
			//First make sure only alpha-numeric characters are in the name
			int numTaxa = taxa.getNumTaxa();
			int totalDigits = (int)(Math.log10((double)numTaxa)) + 1;
			boolean alphanumeric = true;
			int charCount = 0;
			while(alphanumeric && charCount < name.length()){
				if(!Character.isLetterOrDigit(name.charAt(charCount))){
					alphanumeric = false;
				}
			}
			//If all characters are alpha-numeric, still need to make sure they are the correct length
			if(name.length() != totalDigits + startLength && alphanumeric){
				String s = name.substring(startLength);
				int taxonNumber = MesquiteInteger.fromString(s) - 1;
				return taxonNumber;
			}
		}
		return -1;
	}
}
