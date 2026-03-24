#ifndef HALL_ROTATE_H
#define HALL_ROTATE_H

/**
 * @brief Configure GPIO 34 as digital input for the hall sensor (call once after RelayInit).
 */
void HallRotateInit(void);

/**
 * @brief Turn relay 1 ON (motor), then stop on hall sensor falling edge (GPIO interrupt, not slow polling).
 * Short ignore window after start avoids false triggers; runs in a short-lived task (safe from any task).
 */
void Rotate(void);

#endif /* HALL_ROTATE_H */
