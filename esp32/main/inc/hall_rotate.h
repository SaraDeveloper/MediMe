#ifndef HALL_ROTATE_H
#define HALL_ROTATE_H

/**
 * @brief Configure GPIO 34 as digital input for the hall sensor (call once after RelayInit).
 */
void HallRotateInit(void);

/**
 * @brief Turn relay 1 ON (motor), continuously monitor hall sensor, stop immediately when sensor = 0.
 * Runs asynchronously in a short-lived task (safe to call from any task).
 */
void Rotate(void);

#endif /* HALL_ROTATE_H */
