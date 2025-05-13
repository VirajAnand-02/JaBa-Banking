<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>System Configuration - JaBa Banking Admin</title>
    
    <!-- CSS Stylesheets -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin-styles.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/modal.css">
    
    <!-- Favicon -->
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon">
</head>
<body>
    <!-- Hidden context path for JavaScript use -->
    <input type="hidden" id="contextPath" value="${pageContext.request.contextPath}">
    
    <div class="admin-container">
        <!-- Include the sidebar -->
        <jsp:include page="includes/sidebar.jsp" />
        
        <div class="main-content">
            <!-- Include the header -->
            <jsp:include page="includes/header.jsp" />
            
            <!-- Main content area -->
            <main>
                <div class="page-header">
                    <h1>System Configuration</h1>
                    <p>Manage system interest rates and configurations</p>
                </div>
                
                <!-- System Stats Summary -->
                <div class="stats-container">
                    <div class="stat-card">
                        <h3>Current Savings Rate</h3>
                        <p id="currentSavingsRate">Loading...</p>
                    </div>
                    <div class="stat-card">
                        <h3>Current Personal Loan Rate</h3>
                        <p id="currentPersonalLoanRate">Loading...</p>
                    </div>
                    <div class="stat-card">
                        <h3>Current Home Loan Rate</h3>
                        <p id="currentHomeLoanRate">Loading...</p>
                    </div>
                    <div class="stat-card">
                        <h3>Last Updated</h3>
                        <p id="lastUpdated">Loading...</p>
                    </div>
                </div>
                
                <!-- Interest Rate Configuration Section -->
                <div class="admin-card">
                    <div class="card-header">
                        <h2>Interest Rate Configuration</h2>
                        <p>Adjust interest rates for various account types. Changes will apply to new accounts and loans only.</p>
                    </div>
                    
                    <div class="card-body">
                        <form id="interestRateForm" class="admin-form">
                            <div class="form-group">
                                <h3>Savings Account Rates</h3>
                                <div class="form-row">
                                    <div class="form-field">
                                        <label for="basicSavingsRate">Basic Savings Rate (%)</label>
                                        <input type="number" id="basicSavingsRate" name="basicSavingsRate" step="0.01" required>
                                    </div>
                                    <div class="form-field">
                                        <label for="premiumSavingsRate">Premium Savings Rate (%)</label>
                                        <input type="number" id="premiumSavingsRate" name="premiumSavingsRate" step="0.01" required>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <h3>Loan Interest Rates</h3>
                                <div class="form-row">
                                    <div class="form-field">
                                        <label for="personalLoanRate">Personal Loan Rate (%)</label>
                                        <input type="number" id="personalLoanRate" name="personalLoanRate" step="0.01" required>
                                    </div>
                                    <div class="form-field">
                                        <label for="homeLoanRate">Home Loan Rate (%)</label>
                                        <input type="number" id="homeLoanRate" name="homeLoanRate" step="0.01" required>
                                    </div>
                                </div>
                                <div class="form-row">
                                    <div class="form-field">
                                        <label for="autoLoanRate">Auto Loan Rate (%)</label>
                                        <input type="number" id="autoLoanRate" name="autoLoanRate" step="0.01" required>
                                    </div>
                                    <div class="form-field">
                                        <label for="businessLoanRate">Business Loan Rate (%)</label>
                                        <input type="number" id="businessLoanRate" name="businessLoanRate" step="0.01" required>
                                    </div>
                                </div>
                                <div class="form-row">
                                    <div class="form-field">
                                        <label for="educationLoanRate">Education Loan Rate (%)</label>
                                        <input type="number" id="educationLoanRate" name="educationLoanRate" step="0.01" required>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <h3>System Parameters</h3>
                                <div class="form-row">
                                    <div class="form-field">
                                        <label for="minimumBalance">Minimum Balance Requirement ($)</label>
                                        <input type="number" id="minimumBalance" name="minimumBalance" step="0.01" required>
                                    </div>
                                    <div class="form-field">
                                        <label for="transactionFee">Transaction Fee ($)</label>
                                        <input type="number" id="transactionFee" name="transactionFee" step="0.01" required>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="form-actions">
                                <button type="submit" class="action-button primary">Save Configuration</button>
                                <button type="reset" class="action-button secondary">Reset</button>
                            </div>
                        </form>
                    </div>
                </div>
                
                <!-- Change History Section -->
                <div class="admin-card">
                    <div class="card-header">
                        <h2>Configuration Change History</h2>
                    </div>
                    <div class="card-body">
                        <table class="admin-table">
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Admin User</th>
                                    <th>Parameter</th>
                                    <th>Old Value</th>
                                    <th>New Value</th>
                                </tr>
                            </thead>
                            <tbody id="configHistoryTableBody">
                                <tr>
                                    <td colspan="5" class="loading-indicator">Loading history...</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </main>
            
            <!-- Include the footer -->
            <jsp:include page="includes/footer.jsp" />
        </div>
    </div>
    
    <!-- Confirmation Modal -->
    <div id="confirmationModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Confirm Changes</h2>
                <span class="close-modal" onclick="closeConfirmationModal()">&times;</span>
            </div>
            <div class="modal-body">
                <p>Are you sure you want to update the system configuration? These changes will affect all new accounts and loans.</p>
                <div id="changesSummary" class="changes-summary">
                    <!-- Changes will be dynamically inserted here -->
                </div>
            </div>
            <div class="modal-footer">
                <button class="action-button danger" onclick="submitConfigChanges()">Confirm Changes</button>
                <button class="action-button secondary" onclick="closeConfirmationModal()">Cancel</button>
            </div>
        </div>
    </div>
    
    <!-- JavaScript files -->
    <script src="${pageContext.request.contextPath}/js/admin/system-config.js"></script>
</body>
</html>
