package mesquite.zephyr.lib;

import mesquite.lib.TaxaSelectionSet;

public interface NewTreeProcessor {

	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet);

}
