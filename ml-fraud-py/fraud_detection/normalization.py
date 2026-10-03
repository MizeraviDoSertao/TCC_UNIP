from __future__ import annotations

import re
import unicodedata
from collections.abc import Mapping
from typing import Any

import numpy as np
import pandas as pd


def normalize_name(value: str) -> str:
  without_accents = "".join(
    character
    for character in unicodedata.normalize("NFD", str(value))
    if unicodedata.category(character) != "Mn"
  )
  return re.sub(r"^_+|_+$", "", re.sub(r"[^a-z0-9]+", "_", without_accents.lower().strip()))


def normalize_frame_columns(frame: pd.DataFrame) -> pd.DataFrame:
  normalized = [normalize_name(column) for column in frame.columns]
  if any(not column for column in normalized):
    raise ValueError("Dataset contains a column name that cannot be normalized")
  duplicates = sorted({column for column in normalized if normalized.count(column) > 1})
  if duplicates:
    raise ValueError(f"Dataset contains duplicated normalized columns: {', '.join(duplicates)}")
  result = frame.copy()
  result.columns = normalized
  return result


def normalize_feature_mapping(features: Mapping[str, Any]) -> dict[str, Any]:
  normalized: dict[str, Any] = {}
  for key, value in features.items():
    normalized_key = normalize_name(str(key))
    if not normalized_key:
      raise ValueError(f"Feature name cannot be normalized: {key}")
    if normalized_key in normalized:
      raise ValueError(f"Duplicated normalized feature: {normalized_key}")
    normalized[normalized_key] = _json_safe_value(value)
  return normalized


def find_target_column(columns: list[str], aliases: tuple[str, ...]) -> str | None:
  normalized_aliases = {normalize_name(alias) for alias in aliases}
  matches = [column for column in columns if column in normalized_aliases]
  if len(matches) > 1:
    raise ValueError(f"Dataset contains multiple fraud target columns: {', '.join(matches)}")
  return matches[0] if matches else None


def parse_binary_target(series: pd.Series) -> pd.Series:
  positive = {"1", "true", "yes", "sim", "fraud", "fraude"}
  negative = {"0", "false", "no", "nao", "não", "legitimate", "legitimo", "legítimo"}

  def parse(value: Any) -> int | None:
    if pd.isna(value):
      return None
    if isinstance(value, bool):
      return int(value)
    if isinstance(value, (int, float)):
      if float(value) == 1.0:
        return 1
      if float(value) == 0.0:
        return 0
      raise ValueError(f"Invalid numeric fraud label: {value}")
    normalized = str(value).strip().lower()
    if normalized in positive:
      return 1
    if normalized in negative:
      return 0
    raise ValueError(f"Invalid fraud label: {value}")

  return series.map(parse).astype("Int64")


def coerce_feature_types(
    frame: pd.DataFrame,
) -> tuple[pd.DataFrame, tuple[str, ...], tuple[str, ...]]:
  result = frame.copy()
  numeric: list[str] = []
  categorical: list[str] = []
  for column in list(result.columns):
    if result[column].notna().sum() == 0:
      result = result.drop(columns=[column])
      continue
    if pd.api.types.is_bool_dtype(result[column]):
      result[column] = _categorical_series(result[column])
      categorical.append(column)
      continue
    converted = pd.to_numeric(result[column], errors="coerce")
    non_null = result[column].notna().sum()
    convertible = converted.notna().sum()
    if pd.api.types.is_numeric_dtype(result[column]) or (
        non_null > 0 and convertible / non_null >= 0.95
    ):
      result[column] = converted.replace([np.inf, -np.inf], np.nan)
      numeric.append(column)
    else:
      result[column] = _categorical_series(result[column])
      categorical.append(column)
  return result, tuple(numeric), tuple(categorical)


def align_features(features: Mapping[str, Any], columns: tuple[str, ...]) -> pd.DataFrame:
  normalized = normalize_feature_mapping(features)
  return pd.DataFrame([{column: normalized.get(column) for column in columns}], columns=columns)


def prepare_inference_frame(
    features: Mapping[str, Any],
    columns: tuple[str, ...],
    numeric_columns: tuple[str, ...],
    categorical_columns: tuple[str, ...],
) -> pd.DataFrame:
  frame = align_features(features, columns)
  for column in numeric_columns:
    frame[column] = pd.to_numeric(frame[column], errors="coerce")
  for column in categorical_columns:
    frame[column] = _categorical_series(frame[column])
  return frame


def _json_safe_value(value: Any) -> Any:
  if value is None or isinstance(value, (str, int, float, bool)):
    return value
  return str(value)


def _categorical_series(series: pd.Series) -> pd.Series:
  values = series.astype("object")
  return values.where(values.notna(), np.nan)
