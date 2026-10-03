from __future__ import annotations

from pathlib import Path
from typing import Protocol

import pandas as pd

from fraud_detection.domain import ModelBundle


class DatasetReader(Protocol):
  def read(self, path: Path) -> pd.DataFrame: ...


class ModelRepository(Protocol):
  def save(self, bundle: ModelBundle, path: Path) -> None: ...

  def load(self, path: Path) -> ModelBundle: ...


class TrainingStrategy(Protocol):
  def train(
      self,
      features: pd.DataFrame,
      target: pd.Series | None,
      target_column: str | None,
  ) -> ModelBundle: ...
