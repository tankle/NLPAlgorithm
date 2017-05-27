package io.github.tankle.datastructs;

import java.util.List;

public interface MarkRltFilter {
	public List<BoundInfo> filter(List<BoundInfo> from_mark_rlt) throws Exception;
}
