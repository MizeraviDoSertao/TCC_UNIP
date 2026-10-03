from __future__ import annotations

import json
import os
from pathlib import Path
from tempfile import NamedTemporaryFile

import joblib

from fraud_detection.domain import ModelBundle


class JoblibModelRepository:
    def save(self, bundle: ModelBundle, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary_path: Path | None = None
        try:
            with NamedTemporaryFile(
                dir=path.parent, suffix=".joblib.tmp", delete=False
            ) as temporary:
                temporary_path = Path(temporary.name)
            joblib.dump(bundle, temporary_path)
            os.replace(temporary_path, path)
            temporary_path = None
            self._save_manifest(bundle, path.with_suffix(path.suffix + ".metadata.json"))
        finally:
            if temporary_path is not None:
                temporary_path.unlink(missing_ok=True)

    def load(self, path: Path) -> ModelBundle:
        if not path.is_file():
            raise FileNotFoundError(
                f"Model artifact not found: {path}. Run the training command first."
            )
        bundle = joblib.load(path)
        if not isinstance(bundle, ModelBundle):
            raise TypeError("Model artifact has an unsupported format; retrain the model")
        return bundle

    def _save_manifest(self, bundle: ModelBundle, path: Path) -> None:
        manifest = {
            "modelVersion": bundle.model_version,
            "modelType": bundle.model_type.value,
            "trainedAt": bundle.trained_at,
            "datasetSha256": bundle.dataset_sha256,
            "targetColumn": bundle.target_column,
            "threshold": bundle.threshold,
            "featureColumns": list(bundle.feature_columns),
            "numericColumns": list(bundle.numeric_columns),
            "categoricalColumns": list(bundle.categorical_columns),
            "metrics": bundle.metrics,
        }
        temporary_path: Path | None = None
        try:
            with NamedTemporaryFile(
                mode="w",
                encoding="utf-8",
                dir=path.parent,
                suffix=".json.tmp",
                delete=False,
            ) as temporary:
                temporary_path = Path(temporary.name)
                json.dump(manifest, temporary, indent=2, ensure_ascii=False, allow_nan=False)
                temporary.write("\n")
            os.replace(temporary_path, path)
            temporary_path = None
        finally:
            if temporary_path is not None:
                temporary_path.unlink(missing_ok=True)
