package mesquite.zephyr.lib;

import mesquite.lib.taxa.TaxaSelectionSet;

public interface NewTreeProcessor {

	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet);

}
