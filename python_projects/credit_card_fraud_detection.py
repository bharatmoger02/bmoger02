# Credit Card Fraud Detection using Deep Learning
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import confusion_matrix, classification_report
from sklearn.metrics import roc_auc_score, roc_curve, precision_recall_curve
from imblearn.over_sampling import SMOTE
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, models
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau


# 1. Load and Explore Data
def load_data(filepath='creditcard.csv'):
    """Load the credit card dataset"""
    df = pd.read_csv(filepath)
    print("Dataset Shape:", df.shape)
    print("\nClass Distribution:")
    print(df['Class'].value_counts())
    print("\nFraud Percentage:", df['Class'].sum() / len(df) * 100, "%")
    return df


# 2. Data Preprocessing
def preprocess_data(df):
    """Preprocess and scale the data"""
    # Separate features and target
    X = df.drop('Class', axis=1)
    y = df['Class']

    # Split the data
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    # Scale the features
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    return X_train_scaled, X_test_scaled, y_train, y_test, scaler


# 3. Handle Imbalanced Data using SMOTE
def balance_data(X_train, y_train):
    """Apply SMOTE to balance the dataset"""
    print("Before SMOTE:", y_train.value_counts())
    smote = SMOTE(random_state=42)
    X_train_balanced, y_train_balanced = smote.fit_resample(X_train, y_train)
    print("After SMOTE:", pd.Series(y_train_balanced).value_counts())
    return X_train_balanced, y_train_balanced


# 4. Build Deep Learning Model
def build_model(input_dim):
    """Build a deep neural network for fraud detection"""
    model = models.Sequential([
        layers.Dense(128, activation='relu', input_shape=(input_dim,)),
        layers.Dropout(0.3),
        layers.BatchNormalization(),

        layers.Dense(64, activation='relu'),
        layers.Dropout(0.3),
        layers.BatchNormalization(),

        layers.Dense(32, activation='relu'),
        layers.Dropout(0.2),
        layers.BatchNormalization(),

        layers.Dense(16, activation='relu'),
        layers.Dropout(0.2),

        layers.Dense(1, activation='sigmoid')
    ])

    model.compile(
        optimizer='adam',
        loss='binary_crossentropy',
        metrics=['accuracy', tf.keras.metrics.Precision(),
                 tf.keras.metrics.Recall(), tf.keras.metrics.AUC()]
    )

    return model


# 5. Train the Model
def train_model(model, X_train, y_train, X_test, y_test):
    """Train the model with callbacks"""
    # Callbacks
    early_stopping = EarlyStopping(
        monitor='val_loss',
        patience=10,
        restore_best_weights=True
    )

    reduce_lr = ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=5,
        min_lr=1e-7
    )

    # Train
    history = model.fit(
        X_train, y_train,
        validation_data=(X_test, y_test),
        epochs=50,
        batch_size=256,
        callbacks=[early_stopping, reduce_lr],
        verbose=1
    )

    return history


# 6. Evaluate Model
def evaluate_model(model, X_test, y_test):
    """Comprehensive model evaluation"""
    y_pred_prob = model.predict(X_test)
    y_pred = (y_pred_prob > 0.5).astype(int)

    # Confusion Matrix
    cm = confusion_matrix(y_test, y_pred)
    print("\nConfusion Matrix:")
    print(cm)

    # Classification Report
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))

    # ROC-AUC Score
    roc_auc = roc_auc_score(y_test, y_pred_prob)
    print(f"\nROC-AUC Score: {roc_auc:.4f}")

    return y_pred, y_pred_prob, cm


# 7. Visualize Results
def plot_results(history, y_test, y_pred_prob):
    """Plot training history and ROC curve"""
    fig, axes = plt.subplots(2, 2, figsize=(15, 10))

    # Accuracy
    axes[0, 0].plot(history.history['accuracy'], label='Train')
    axes[0, 0].plot(history.history['val_accuracy'], label='Validation')
    axes[0, 0].set_title('Model Accuracy')
    axes[0, 0].set_xlabel('Epoch')
    axes[0, 0].set_ylabel('Accuracy')
    axes[0, 0].legend()

    # Loss
    axes[0, 1].plot(history.history['loss'], label='Train')
    axes[0, 1].plot(history.history['val_loss'], label='Validation')
    axes[0, 1].set_title('Model Loss')
    axes[0, 1].set_xlabel('Epoch')
    axes[0, 1].set_ylabel('Loss')
    axes[0, 1].legend()

    # ROC Curve
    fpr, tpr, _ = roc_curve(y_test, y_pred_prob)
    axes[1, 0].plot(fpr, tpr, label=f'AUC = {roc_auc_score(y_test, y_pred_prob):.4f}')
    axes[1, 0].plot([0, 1], [0, 1], 'k--')
    axes[1, 0].set_title('ROC Curve')
    axes[1, 0].set_xlabel('False Positive Rate')
    axes[1, 0].set_ylabel('True Positive Rate')
    axes[1, 0].legend()

    # Precision-Recall Curve
    precision, recall, _ = precision_recall_curve(y_test, y_pred_prob)
    axes[1, 1].plot(recall, precision)
    axes[1, 1].set_title('Precision-Recall Curve')
    axes[1, 1].set_xlabel('Recall')
    axes[1, 1].set_ylabel('Precision')

    plt.tight_layout()
    plt.show()


# 8. Main Execution
def main():
    """Main execution function"""
    # Load data
    df = load_data('creditcard.csv')

    # Preprocess
    X_train, X_test, y_train, y_test, scaler = preprocess_data(df)

    # Balance data
    X_train_balanced, y_train_balanced = balance_data(X_train, y_train)

    # Build model
    model = build_model(X_train_balanced.shape[1])
    print("\nModel Architecture:")
    model.summary()

    # Train model
    history = train_model(model, X_train_balanced, y_train_balanced,
                          X_test, y_test)

    # Evaluate
    y_pred, y_pred_prob, cm = evaluate_model(model, X_test, y_test)

    # Visualize
    plot_results(history, y_test, y_pred_prob)

    # Save model
    model.save('fraud_detection_model.h5')
    print("\nModel saved as 'fraud_detection_model.h5'")

    return model, scaler


# Run the project
if __name__ == "__main__":
    model, scaler = main()


# 9. Prediction Function
def predict_fraud(model, scaler, transaction_data):
    """Predict if a transaction is fraudulent"""
    transaction_scaled = scaler.transform([transaction_data])
    prediction_prob = model.predict(transaction_scaled)[0][0]
    prediction = "FRAUD" if prediction_prob > 0.5 else "LEGITIMATE"
    return prediction, prediction_prob