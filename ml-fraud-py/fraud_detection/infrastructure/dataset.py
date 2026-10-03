from __future__ import annotations

from pathlib import Path

import pandas as pd


class PandasDatasetReader:
  def read(self, path: Path) -> pd.DataFrame:
    if not path.is_file():
      raise FileNotFoundError(f"Dataset not found: {path}")
    suffix = path.suffix.lower()
    if suffix == ".csv":
      return pd.read_csv(path, sep=None, engine="python")
    if suffix in {".xlsx", ".xls"}:
      sheets = pd.read_excel(path, sheet_name=None)
      non_empty = [frame for frame in sheets.values() if not frame.empty]
      if not non_empty:
        return pd.DataFrame()
      return pd.concat(non_empty, ignore_index=True, sort=False)
    raise ValueError("Supported dataset formats: .csv, .xlsx and .xls")
