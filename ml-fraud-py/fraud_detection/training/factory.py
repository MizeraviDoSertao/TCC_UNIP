from __future__ import annotations

from fraud_detection.ports import TrainingStrategy
from fraud_detection.training.anomaly import AnomalyTrainingStrategy
from fraud_detection.training.supervised import SupervisedTrainingStrategy


class TrainingStrategyFactory:
    def __init__(self, random_state: int, test_size: float, contamination: float) -> None:
        self._random_state = random_state
        self._test_size = test_size
        self._contamination = contamination

    def create(self, has_target: bool) -> TrainingStrategy:
        if has_target:
            return SupervisedTrainingStrategy(self._random_state, self._test_size)
        return AnomalyTrainingStrategy(self._random_state, self._contamination)
