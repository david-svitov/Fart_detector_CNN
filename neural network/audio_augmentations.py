import librosa
import numpy as np


def change_pitch(
        sample: np.ndarray,
        sample_rate: int = 16000,
) -> np.ndarray:
    y_pitch = sample.copy()
    bins_per_octave = 12
    pitch_pm = 2
    pitch_change = pitch_pm * 2 * (np.random.uniform())
    y_pitch = librosa.effects.pitch_shift(
        y_pitch.astype('float64'),
        sample_rate,
        n_steps=pitch_change,
        bins_per_octave=bins_per_octave,
    )
    return y_pitch


def change_speed(
        sample: np.ndarray,
) -> np.ndarray:
    y_speed = sample.copy()
    speed_change = np.random.uniform(low=0.9, high=1.1)
    tmp = librosa.effects.time_stretch(
        y_speed.astype('float64'),
        speed_change,
    )
    minlen = min(y_speed.shape[0], tmp.shape[0])
    y_speed *= 0
    y_speed[0:minlen] = tmp[0:minlen]
    return y_speed


def change_value(
        sample: np.ndarray,
) -> np.ndarray:
    y_aug = sample.copy()
    dyn_change = np.random.uniform(low=1.5, high=3)
    y_aug = y_aug * dyn_change
    return y_aug


def change_noise(
        sample: np.ndarray,
) -> np.ndarray:
    y_noise = sample.copy()
    noise_amp = 0.005*np.random.uniform()*np.amax(y_noise)
    y_noise = y_noise.astype('float64') + noise_amp * np.random.normal(size=y_noise.shape[0])
    return y_noise
