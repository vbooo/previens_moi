import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("observation", Path(__file__).resolve().parents[1] / "observe-service.py")
observation = importlib.util.module_from_spec(spec)
spec.loader.exec_module(observation)


class ObservationBudgetTest(unittest.TestCase):
    def test_seven_daily_checks_include_preparation_and_a_silent_baseline(self):
        plan = observation.forecast(days=7, interval_hours=24, sources=3)
        self.assertEqual(plan["scheduledChecks"], 7)
        # A 30-credit trial can reserve discovery + preparation + seven daily checks.
        self.assertLessEqual(plan["webCreditReservationUpper"], 30)
        self.assertGreater(plan["webCreditReservationUpper"], 7 * 3)
        self.assertEqual(plan["modelCallsUpper"], 10)
        self.assertFalse(plan["requiresCadencePolicyChange"])

    def test_hourly_trial_cannot_be_advertised_as_the_same_allowance(self):
        daily = observation.forecast(interval_hours=24)
        hourly = observation.forecast(interval_hours=1)
        self.assertEqual(hourly["scheduledChecks"], 168)
        # Rediscovery and preparation cost more than the 504 retained-page reads.
        self.assertGreater(hourly["webCreditReservationUpper"], 504)
        self.assertGreater(hourly["modelReservationMicroUsd"], 2_000_000)
        self.assertGreater(hourly["webCreditReservationUpper"], daily["webCreditReservationUpper"])

    def test_invalid_or_unmodelled_cadences_are_not_forecast_as_supported(self):
        for settings in ({"days": 0}, {"sources": 0}, {"sources": 4}, {"interval_hours": 6}):
            with self.subTest(settings=settings), self.assertRaises(ValueError):
                observation.forecast(**settings)


if __name__ == "__main__":
    unittest.main()
